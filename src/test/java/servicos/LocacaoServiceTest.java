package servicos;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static utils.DataUtils.isMesmaData;
import static utils.DataUtils.obterDataComDiferencaDias;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import daos.LocacaoDAO;
import daos.SPCService;
import entidades.Filme;
import entidades.Locacao;
import entidades.Usuario;
import exceptions.FilmeSemEstoqueException;
import exceptions.LocadoraException;
import utils.DataUtils;

public class LocacaoServiceTest {
	
	private LocacaoService service;
	
	private SPCService spc;
	private LocacaoDAO dao;
	
	@Rule
	public ErrorCollector error = new ErrorCollector();
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setup() {
		service = new LocacaoService();
		dao = Mockito.mock(LocacaoDAO.class);
		service.setLocacaoDAO(dao);
		spc = Mockito.mock(SPCService.class);
		service.setSPCService(spc);
	}
	
	@Test
	public void deveAlugarFilme() throws Exception {
		Assume.assumeFalse(DataUtils.verificarDiaSemana(new Date(), Calendar.SATURDAY));
		
		//cenario
		Usuario usuario = new Usuario("Usuario 1");
		List<Filme> filmes = Arrays.asList(new Filme("Filme 1", 2, 5.0));
		
		//acao
		Locacao locacao = service.alugarFilme(usuario, filmes);
		
		//verificacao
		error.checkThat(locacao.getValor(), is(equalTo(5.0)));
		error.checkThat(isMesmaData(locacao.getDataLocacao(), new Date()), is(true));
		error.checkThat(isMesmaData(locacao.getDataRetorno(), obterDataComDiferencaDias(1)), is(true));
	}
	
	@Test(expected = FilmeSemEstoqueException.class)
	public void naoDeveAlugarFilmeSemEstoque() throws Exception {
		//cenario
		Usuario usuario = new Usuario("Usuario 1");
		List<Filme> filmes = Arrays.asList(new Filme("Filme 1", 0, 4.0));
				
		//acao
		service.alugarFilme(usuario, filmes);
	}
	
	@Test
	public void naoDeveAlugarFilmeSemUsuario() throws FilmeSemEstoqueException {
		//cenario
		List<Filme> filmes = Arrays.asList(new Filme("Filme 1", 2, 5.0));
		
		//acao		
		try {
			service.alugarFilme(null, filmes);
			Assert.fail();
		} catch (LocadoraException e) {
			assertThat(e.getMessage(), is("Usuario vazio"));
		}
	}
	
	@Test
	public void naoDeveAlugarFilmeSemFilme() throws FilmeSemEstoqueException, LocadoraException {
		//cenario
		Usuario usuario = new Usuario("Usuario 1");
		
		exception.expect(LocadoraException.class);
		exception.expectMessage("Filme vazio");
		
		//acao
		service.alugarFilme(usuario, null);
	}
	
	@Test
	public void devePagar75PorcentoNoTerceiroFilme() throws FilmeSemEstoqueException, LocadoraException {
		//cenario
		Usuario usuario = new Usuario("Usuario 1");
		List<Filme> filmes = Arrays.asList(new Filme("Filme 1", 2, 4.0), new Filme("Filme 2", 1, 4.0),
				new Filme("Filme 3", 3, 4.0));
	
		//acao
		Locacao resultado = service.alugarFilme(usuario, filmes);
		
		//verificacao
		assertThat(resultado.getValor(), is(11.0));
	}
	
	@Test
	public void devePagar50PorcentoNoQuartoFilme() throws FilmeSemEstoqueException, LocadoraException {
		//cenario
		Usuario usuario = new Usuario("Usuario 1");
		List<Filme> filmes = Arrays.asList(
				new Filme("Filme 1", 2, 4.0), new Filme("Filme 2", 1, 4.0),
				new Filme("Filme 3", 3, 4.0), new Filme("Filme 4", 1, 4.0));
	
		//acao
		Locacao resultado = service.alugarFilme(usuario, filmes);
		
		//verificacao
		assertThat(resultado.getValor(), is(13.0));
	}
	
	@Test
	public void devePagar25PorcentoNoQuintoFilme() throws FilmeSemEstoqueException, LocadoraException {
		//cenario
		Usuario usuario = new Usuario("Usuario 1");
		List<Filme> filmes = Arrays.asList(
				new Filme("Filme 1", 2, 4.0), new Filme("Filme 2", 1, 4.0),
				new Filme("Filme 3", 3, 4.0), new Filme("Filme 4", 1, 4.0),
				new Filme("Filme 5", 3, 4.0));
	
		//acao
		Locacao resultado = service.alugarFilme(usuario, filmes);
		
		//verificacao
		assertThat(resultado.getValor(), is(14.0));
	}
	
	@Test
	public void naodevePagarPeloSextoFilme() throws FilmeSemEstoqueException, LocadoraException {
		//cenario
		Usuario usuario = new Usuario("Usuario 1");
		List<Filme> filmes = Arrays.asList(
				new Filme("Filme 1", 2, 4.0), new Filme("Filme 2", 1, 4.0),
				new Filme("Filme 3", 3, 4.0), new Filme("Filme 4", 1, 4.0),
				new Filme("Filme 5", 5, 4.0), new Filme("Filme 6", 3, 4.0));
	
		//acao
		Locacao resultado = service.alugarFilme(usuario, filmes);
		
		//verificacao
		//4+4+3+2+1+0 = 14
		assertThat(resultado.getValor(), is(14.0));
	}
	
	@Test
	public void deveDevolverFilmeNaSegundaAoAlugarNoSabado() throws FilmeSemEstoqueException, LocadoraException {
		Assume.assumeTrue(DataUtils.verificarDiaSemana(new Date(), Calendar.SATURDAY));
		
		//cenario
		Usuario usuario = new Usuario("Usuario 1");
		List<Filme> filmes = Arrays.asList(new Filme("Filme 1", 2, 5.0));
		
		//acao
		Locacao retorno = service.alugarFilme(usuario, filmes);
		
		//verificacao
		boolean ehSegunda = DataUtils.verificarDiaSemana(retorno.getDataRetorno(), Calendar.MONDAY);
		Assert.assertTrue(ehSegunda);
	}
	
	@Test
	public void naoDeveAlugarFilmeParaNegativadoSPC() throws FilmeSemEstoqueException, LocadoraException {
		//cenario
		Usuario usuario = new Usuario("Usuario 1");
		List<Filme> filmes = Arrays.asList(new Filme("Filme 1", 2, 5.0));
		
		Mockito.when(spc.possuiNegativacao(usuario)).thenReturn(true);
		
		exception.expect(LocadoraException.class);
		exception.expectMessage("Usuário Negativado");
		
		//acao
		service.alugarFilme(usuario, filmes);
	}
}

