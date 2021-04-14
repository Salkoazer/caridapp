
import com.google.gson.Gson;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import spark.Request;
import utils.freemarker.FreemarkerEngine;
import static spark.Spark.*;
import utils.freemarker.FreemarkerContext;
import utils.mysql.DBRow;
import utils.mysql.DBRowList;
import utils.mysql.MySQLConn;

public class Application {

    static MySQLConn conn = MySQLConn.getSharedInstance();



    public static void main(String[] args) {

        // Initialize MySQL connection
        conn.init("localhost", "3306", "username", "password", "caridapp");

        // Configure Spark
        port(8000);

        // porquê externalLocation em vez de location????
        staticFiles.externalLocation("src/resources");

        // Configure freemarker engine
        FreemarkerEngine engine = new FreemarkerEngine("src/resources/templates");
        
        // Set up gson
        Gson gson = new Gson();

        // Home Page
        get("/", (request, response) -> {
            FreemarkerContext context = new FreemarkerContext();
            context.put("user", request.session().attribute("user"));

            response.redirect("/index/");
            return "";
        });

        //página inicial da App, que redireciona para os temas passados 3 seg
        get("/index/", (request, response) -> {
            FreemarkerContext context = new FreemarkerContext();
            context.put("user", request.session().attribute("user"));

            return engine.render(context, "index.html");
        });

        // temas
        get("/temas/", (request, response) -> {
            FreemarkerContext context = new FreemarkerContext();
            context.put("user", request.session().attribute("user"));

            //nomes dos temas
            String sql = "select * from theme";
            DBRowList temas = conn.executeQuery(sql);

            context.put("temas", temas);

            return engine.render(context, "temas.html");
        });

        //Lista de projetos de um tema
        get("/temas/:themeid/", (request, response) -> {

            FreemarkerContext context = new FreemarkerContext();
            context.put("user", request.session().attribute("user"));

            int id = Integer.parseInt(request.params(":themeid"));
            int pNumber = Integer.parseInt(request.queryParams("p"));
            context.put("pagina_atual", pNumber);
            String sql = String.format("SELECT * FROM theme WHERE id_theme=%s", id);
            DBRow tema = conn.executeQuery(sql).first();
            context.put("theme", tema);

            sql = String.format("SELECT COUNT(projectID) AS count FROM project WHERE id_theme = %s", id);
            DBRow count = conn.executeQuery(sql).first();

            Long n = (Long) count.get("count");
            int nProjects = n.intValue();

            //nomes dos projetos que pertencem ao tema com este id_theme
            sql = String.format("select * from project WHERE id_theme=%s", id);
            DBRowList projects = conn.executeQuery(sql);

            List<DBRow> fracaoProjects = null;

            int nPaginas = 0;

            if (projects.size() > 0) {

                context.put("has_projects", true);

                if (nProjects % 12 != 0) {
                    nPaginas = nProjects / 12 + 1;
                } else {
                    nPaginas = nProjects / 12;
                }

                if (nPaginas == pNumber) {
                    if (nProjects % 12 != 0) {
                        fracaoProjects = projects.subList(((pNumber - 1) * 12), (((pNumber - 1) * 12) + (nProjects % 12)));
                    } else {
                        fracaoProjects = projects.subList(((pNumber - 1) * 12), (pNumber * 12));
                    }
                } else {
                    fracaoProjects = projects.subList(((pNumber - 1) * 12), ((pNumber - 1) * 12 + 12));
                }
            }

            List<Integer> page = new ArrayList<>();

            for (int i = 0; i < nPaginas; i++) {
                if (i < 24) {
                    page.add(i + 1);
                }
            }

            context.put("projects", fracaoProjects);
            if (pNumber + 1 > nPaginas) {
                context.put("next", pNumber);
            } else {
                context.put("next", pNumber + 1);
            }
            if (pNumber - 1 < 1) {
                context.put("previous", 1);
            } else {
                context.put("previous", pNumber - 1);
            }

            if (nPaginas > 1) {
                context.put("pagination", true);
            }

            context.put("nPaginas", page);

            request.session().attribute("theme", tema);

            return engine.render(context, "listaProjetos.html");
        });

        //Página de um projeto em particular
        get("/temas/:themeid/projeto/:id", (request, response) -> {

            FreemarkerContext context = new FreemarkerContext();
            DBRow user = request.session().attribute("user");
            context.put("user", user);

            int id = Integer.parseInt(request.params(":id"));
            int themeid = Integer.parseInt(request.params(":themeid"));

            if (request.session().attribute("user") == null) {
                context.put("user_null", true);
            } else {
                if (user != null) {
                    String sql = String.format("SELECT * FROM favorite WHERE userID=%s AND projectID=%s", user.get("userID"), id);
                    if (conn.executeQuery(sql).first() != null) {
                        context.put("favorite", true);
                    }
                }
            }

            DBRow tema = conn.executeQuery(String.format("SELECT * FROM theme WHERE id_theme=%s", themeid)).first();

            String sql = String.format("SELECT * FROM project WHERE projectID=%s", id);
            DBRow projeto = conn.executeQuery(sql).first();

            sql = String.format("SELECT * FROM media WHERE projectID = %s", id);
            DBRowList media = conn.executeQuery(sql);

            sql = String.format("SELECT * FROM project p join organization o on p.orgID=o.orgID WHERE p.projectID=%s", id);
            DBRow organizacao = conn.executeQuery(sql).first();

            int imageCount = media.size();

            int num = 0;
            for (DBRow m : media) {
                m.put("index", ++num);
            }

            context.put("media", media);
            context.put("theme", tema);
            context.put("organizacao", organizacao);
            context.put("imageCount", imageCount);
            context.put("project", projeto);
            request.session().attribute("project", projeto);

            return engine.render(context, "projeto.html");

        });

        // adicionar favoritos
        post("/temas/:themeid/projeto/:id", (request, response) -> {

            FreemarkerContext context = new FreemarkerContext();

            DBRow user = request.session().attribute("user");
            DBRow project = request.session().attribute("project");

            context.put("user", user);

            context.put("project", project);

            int id = Integer.parseInt(request.params(":id"));
            int themeid = Integer.parseInt(request.params(":themeid"));

            if (request.session().attribute("user") == null) {
                context.put("user_null", true);
            } else {
                if (user != null) {
                    String sql = String.format("SELECT * FROM favorite WHERE userID=%s AND projectID=%s", user.get("userID"), id);
                    if (conn.executeQuery(sql).first() != null) {
                        context.put("favorite", true);
                    }
                }
            }

            if (conn.executeQuery(String.format("SELECT * FROM favorite WHERE userID=%s AND projectID=%s", user.get("userID"), id)).first() != null) {
                String sql = String.format("DELETE FROM favorite WHERE userID=%s AND projectID=%s", user.get("userID"), project.get("projectID"));
                conn.executeUpdate(sql);
                context.remove("favorite");
            } else {
                String sql = String.format("INSERT INTO favorite VALUES (%s, %s)", user.get("userID"), project.get("projectID"));
                conn.executeUpdate(sql);
                context.put("favorite", true);
            }

            DBRow tema = conn.executeQuery(String.format("SELECT * FROM theme WHERE id_theme=%s", themeid)).first();

            String sql = String.format("SELECT * FROM media WHERE projectID = %s", id);
            DBRowList media = conn.executeQuery(sql);

            sql = String.format("SELECT * FROM project p join organization o on p.orgID=o.orgID WHERE p.projectID=%s", id);
            DBRow organizacao = conn.executeQuery(sql).first();

            int imageCount = media.size();

            int num = 0;
            for (DBRow m : media) {
                m.put("index", ++num);
            }

            context.put("media", media);
            context.put("theme", tema);
            context.put("organizacao", organizacao);
            context.put("imageCount", imageCount);

            Thread.sleep(3000);

            return engine.render(context, "projeto.html");

        });

        //Registo
        get("/registo/", (request, response) -> {
            FreemarkerContext context = new FreemarkerContext();
            String sql = "SELECT * FROM country";
            DBRowList countries = conn.executeQuery(sql);

            context.put("countries", countries);

            return engine.render(context, "registo.html");
        });

        //Leitura Dados Registo
        //TODO validação em condições
        post("/registo/", (request, response) -> {
            FreemarkerContext context = new FreemarkerContext();

            String sql = "SELECT * FROM country";
            DBRowList countries = conn.executeQuery(sql);

            context.put("countries", countries);

            String username = request.queryParams("username");
            String password = request.queryParams("password");
            String first_name = request.queryParams("first_name");
            String last_name = request.queryParams("last_name");
            String gender = request.queryParams("gender");
            String postal_code = request.queryParams("postal_code1") + "-" + request.queryParams("postal_code2");
            String email = request.queryParams("email");
            String country = request.queryParams("country");
            String nif = request.queryParams("nif");
            String dataNascimento = request.queryParams("dataNasc");

            sql = String.format("INSERT INTO registered_user (userID, username, pass_word, first_name, last_name, admin, gender, postal_code, email, country, nif, birth_date) VALUES (default, '%s', '%s', '%s', '%s', 0, '%s', '%s', '%s', '%s', '%s', '%s')", username, password,
                    first_name, last_name, gender, postal_code, email, country, nif, dataNascimento);

            if (dataNascimento.equals("")) {
                sql = String.format("INSERT INTO registered_user (userID, username, pass_word, first_name, last_name, admin, gender, postal_code, email, country, nif) VALUES (default, '%s', '%s', '%s', '%s', 0, '%s', '%s', '%s', '%s', '%s')", username, password,
                        first_name, last_name, gender, postal_code, email, country, nif);
            }

            if (conn.executeUpdate(sql) != -1) {
                //Faz logo login
                sql = String.format("select * from registered_user where username = '%s' and pass_word='%s'", username, password);
                DBRow user = conn.executeQuery(sql).first();

                //partir o cód postal nos componentes
                String postal_code1 = conn.executeQuery(String.format("SELECT postal_code FROM registered_user WHERE username = '%s' AND pass_word='%s'", username, password)).first().get("postal_code").toString().substring(0, 4);
                String postal_code2 = conn.executeQuery(String.format("SELECT postal_code FROM registered_user WHERE username = '%s' AND pass_word='%s'", username, password)).first().get("postal_code").toString().substring(5, 8);

                user.put("postal_code2", postal_code2);
                user.put("postal_code1", postal_code1);

                request.session().attribute("user", user);
                response.redirect("/temas/"); // TODO redirecionar para TEMAS
                return "";
            } else {

                context.put("register_error", true);
                return engine.render(context, "registo.html");
            }
        });

        //Login (caso esteja logado vai para os temas)
        get("/login/", (request, response) -> {
            if (request.session().attribute("user") == null) {
                return engine.render(null, "login.html");
            } else {
                response.redirect("/temas/");
            }
            return "";
        });

        // Leitura dados login
        post("/login/", (request, response) -> {
            FreemarkerContext context = new FreemarkerContext();
            context.put("project", request.session().attribute("project"));
            context.put("theme", request.session().attribute("theme"));
            context.put("donationOption", request.session().attribute("donationOption"));

            String username = request.queryParams("username");
            String password = request.queryParams("password");
            String sql = String.format("select * from registered_user where username = '%s' and pass_word='%s'", username, password);
            DBRow user = conn.executeQuery(sql).first();

            if (user != null) {

                //partir o cód postal nos componentes
                String postal_code1 = conn.executeQuery(String.format("SELECT postal_code FROM registered_user WHERE username = '%s' AND pass_word='%s'", username, password)).first().get("postal_code").toString().substring(0, 4);
                String postal_code2 = conn.executeQuery(String.format("SELECT postal_code FROM registered_user WHERE username = '%s' AND pass_word='%s'", username, password)).first().get("postal_code").toString().substring(5, 8);

                user.put("postal_code2", postal_code2);
                user.put("postal_code1", postal_code1);

                request.session().attribute("user", user);
                if (request.session().attribute("donationOption") != null) {
                    DBRow project = request.session().attribute("project");
                    DBRow donationOption = request.session().attribute("donationOption");
                    response.redirect("/donativo/" + project.get("projectID") + "/" + donationOption.get("idDonationOption"));
                } else if (request.session().attribute("project") != null) {
                    DBRow theme = request.session().attribute("theme");
                    DBRow project = request.session().attribute("project");
                    response.redirect("/temas/" + theme.get("id_theme") + "/projeto/" + project.get("projectID"));
                } else {
                    response.redirect("/temas/");
                }
                return "";
            } else {
                context.put("login_error", true);
                return engine.render(context, "login.html");
            }
        });

        //Logout --> Importante retirar todos os atributos da sessão
        get("/logout/", (request, response) -> {
            request.session().removeAttribute("user");
            request.session().removeAttribute("theme");
            request.session().removeAttribute("project");
            request.session().removeAttribute("donationOption");
            response.redirect("/temas/");
            return "";
        });

        // Área de Utilizador
        get("/areaUtilizador/", (request, response) -> {
            FreemarkerContext context = new FreemarkerContext();

            DBRow user = request.session().attribute("user");

            // ADMIN
            if ((boolean) user.get("admin")) {
                context.put("is_admin", true);
            }

            //Favoritos
            String sql = String.format("SELECT * FROM favorite WHERE userID = %s", user.get("userID"));
            DBRowList favoritesID = conn.executeQuery(sql);

            if (!favoritesID.isEmpty()) {
                ArrayList<DBRow> favorites = new ArrayList<>();

                for (DBRow favorite : favoritesID) {
                    sql = String.format("Select * from project where projectID = %s", favorite.get("projectID"));
                    favorites.add(conn.executeQuery(sql).first());
                }
                context.put("favorites", favorites);
                context.put("has_favorites", true);
            } else {
                context.remove("has_favorites");
            }

            // Donativos
            sql = String.format("SELECT  * FROM donation d JOIN project p ON d.projectID = p.projectID WHERE d.userID=%s", user.get("userID"));
            DBRowList donations = conn.executeQuery(sql);

            if (!donations.isEmpty()) {
                context.put("donations", donations);
                // Total dos donativos feitos pelo utilizador
                sql = String.format("SELECT sum(amount) AS sum FROM donation d JOIN registered_user r ON d.userID = r.userID WHERE d.userID =%s",user.get("userID"));
                DBRow totalDonation = conn.executeQuery(sql).first();
                context.put("totalDonation", totalDonation);
            }

            context.put("user", user);




            return engine.render(context, "areaUtilizador.html");

        });

        // Admin
        get("/admin/", (request, response) -> {
            FreemarkerContext context = new FreemarkerContext();

            DBRow user = request.session().attribute("user");
            context.put("user", user);

            boolean admin = false;

            if (user != null) {
                admin = (boolean) user.get("admin");
            }

            if (!admin) {
                response.redirect("/temas/");
            }
            String sql = "SELECT COUNT(userID) AS count FROM registered_user";
            DBRow registered_user = conn.executeQuery(sql).first();
            context.put("registered_user", registered_user);

            sql = "SELECT COUNT(userID) AS count FROM deactivated_user";
            DBRow deactivated_user = conn.executeQuery(sql).first();
            context.put("deactivated_user", deactivated_user);

            sql = "SELECT theme_Name, sum(amount) AS sum FROM donation d"
                    + " JOIN project p ON d.projectID = p.projectID"
                    +" JOIN theme t ON p.id_theme = t.id_theme "
                    +" GROUP BY theme_Name UNION ALL SELECT theme_Name"
                    + ", sum(amount) AS sum FROM deactivated_donation e JOIN project p ON"
                    +" e.projectID = p.projectID "
                    +"JOIN theme t ON p.id_theme = t.id_theme "
                    +"GROUP BY theme_Name ORDER BY theme_Name";

            DBRowList amount_por_tema = conn.executeQuery(sql);
            context.put("amount_por_tema", amount_por_tema);

            sql = "SELECT count(p.projectID) as Cont, t.theme_Name FROM project p"
                    + " JOIN theme t ON p.id_theme = t.id_theme "
                    + "GROUP BY t.theme_Name "
                    + "ORDER BY Cont DESC limit 5";
            DBRowList project_semprojetos = conn.executeQuery(sql);
            context.put("project_semprojetos",project_semprojetos);

            return engine.render(context, "admin.html");
        });

        // Leitura do botão para gerar comprovativos de doação
        post("/areaUtilizador/", (request, response) -> {
            FreemarkerContext context = new FreemarkerContext();
            context.put("user", request.session().attribute("user"));

            int donationID = Integer.parseInt(request.queryParams("donationID"));

            String sql = String.format("SELECT  * FROM donation  WHERE donationID=%s", donationID);
            DBRow donation = conn.executeQuery(sql).first();

            context.put("donation", donation);

            response.redirect("/comprovativo/" + donationID);

            return "";

        });

        //comprovativos de doação
        get("/comprovativo/:id", (request, response) -> {
            FreemarkerContext context = new FreemarkerContext();

            DBRow user = request.session().attribute("user");
            context.put("user", user);

            int donationID = Integer.parseInt(request.params(":id"));

            String sql = String.format("SELECT  * FROM donation  WHERE donationID=%s", donationID);
            DBRow donation = conn.executeQuery(sql).first();

            context.put("donation", donation);

            try {
                if (!user.get("userID").equals(donation.get("userID"))) {
                    response.redirect("/temas/");
                }
            } catch (NullPointerException e) {
                response.redirect("/temas/");
            }

            sql = String.format("SELECT  * FROM donation_option  WHERE projectID=%s AND amount=%s", donation.get("projectID"), donation.get("amount"));
            DBRow donationOption = conn.executeQuery(sql).first();

            return engine.render(context, "comprovativo.html");
        });

        // Desativar Conta --> Apenas transfere o user para outra tabela no mySQL
        get("/contaDesativada/:id", (request, response) -> {

            //inserir numa tabela para contas desativadas depois apagar da main tabela
            FreemarkerContext context = new FreemarkerContext();
            context.put("user", request.session().attribute("user"));

            DBRow user = request.session().attribute("user");

            String sql = String.format("SELECT * FROM donation WHERE userID = %s", user.get("userID"));
            DBRowList doacoes = conn.executeQuery(sql);

            if(!doacoes.isEmpty()){
                for(DBRow doacao : doacoes){
                    sql = String.format("INSERT INTO deactivated_donation (donationID,amount,donation_date,message,userID,projectID,uuid)",
                            doacao.get("donationID"),doacao.get("amount"),doacao.get("donation_date"),doacao.get("message"),doacao.get("userID"),doacao.get("projectID"),doacao.get("uuid"));
                    conn.executeUpdate(sql);
                }
                sql = String.format("DELETE FROM donation WHERE userID = %s",user.get("userID"));
                conn.executeUpdate(sql);
            }

            sql = String.format("INSERT INTO deactivated_user (userID, username, pass_word, first_name, last_name, admin, gender,"
                    + " postal_code, email, country, nif, birth_date) SELECT * FROM registered_user  WHERE userID = '%s'", request.params(":id"));
            conn.executeUpdate(sql);

            sql = String.format("DELETE FROM registered_user WHERE userID = '%s'", request.params(":id"));
            conn.executeUpdate(sql);

            request.session().removeAttribute("user");
            request.session().removeAttribute("theme");
            request.session().removeAttribute("project");
            request.session().removeAttribute("donationOption");

            return engine.render(context, "contaDesativada.html");

        });

        // Editar Dados
        get("/editarDados/:id", (request, response) -> {

            FreemarkerContext context = new FreemarkerContext();
            String sql = "SELECT * FROM country";
            DBRowList countries = conn.executeQuery(sql);

            context.put("countries", countries);
            context.put("user", request.session().attribute("user"));

            return engine.render(context, "editarDados.html");

        });

        // Leitura de dados para Editar
        post("/editarDados/:id", (request, response) -> {
            FreemarkerContext context = new FreemarkerContext();
            DBRow user = request.session().attribute("user");
            context.put("user", user);

            String username = request.queryParams("username");
            String pass_word = request.queryParams("pass_word");
            String first_name = request.queryParams("first_name");
            String last_name = request.queryParams("last_name");
            String gender = request.queryParams("gender");
            String postal_code = request.queryParams("postal_code1") + "-" + request.queryParams("postal_code2");
            String email = request.queryParams("email");
            String country = request.queryParams("country");
            String nif = request.queryParams("nif");
            String birth_date = request.queryParams("dataNasc");

            if (birth_date.charAt(2) == '/') {
                String aux = birth_date.substring(6, 10) + "-" + birth_date.substring(3, 5) + "-" + birth_date.substring(0, 2);
                birth_date = aux;
            }

            String sql;

            if (birth_date.equals("") || birth_date.equals("0000-00-00") || birth_date.charAt(3) > '9') {
                sql = String.format("UPDATE IGNORE registered_user SET username = '%s', pass_word = '%s', first_name = '%s', last_name = '%s'"
                                + ", gender = '%s', postal_code = '%s', email = '%s', country = '%s', nif = '%s', birth_date = '%s' WHERE userID = '%s'", username, pass_word, first_name,
                        last_name, gender, postal_code, email, country, nif, user.get("birth_date"), request.params(":id"));

            } else {
                sql = String.format("UPDATE IGNORE registered_user SET username = '%s', pass_word = '%s', first_name = '%s', last_name = '%s'"
                                + ", gender = '%s', postal_code = '%s', email = '%s', country = '%s', nif = '%s', birth_date = '%s' WHERE userID = '%s'", username, pass_word, first_name,
                        last_name, gender, postal_code, email, country, nif, birth_date, request.params(":id"));
            }

            if (conn.executeUpdate(sql) != -1) {
                context.put("update_is_done", true);
                sql = String.format("select * from registered_user where userID=%s", request.params(":id"));
                user = conn.executeQuery(sql).first();

                context.put("user", user);
                request.session().attribute("user", user);
                //partir o cód postal nos componentes
                String postal_code1 = conn.executeQuery(String.format("SELECT postal_code FROM registered_user WHERE username = '%s'", username)).first().get("postal_code").toString().substring(0, 4);
                String postal_code2 = conn.executeQuery(String.format("SELECT postal_code FROM registered_user WHERE username = '%s'", username)).first().get("postal_code").toString().substring(5, 8);

                user.put("postal_code2", postal_code2);
                user.put("postal_code1", postal_code1);
            } else {
                context.put("edit_error", true);
            }

            return engine.render(context, "editarDados.html");

        });

        // Opções de donativo para um projeto em particular
        get("/donativo/:idProjeto", (request, response) -> {

            FreemarkerContext context = new FreemarkerContext();
            DBRow project = request.session().attribute("project");
            context.put("user", request.session().attribute("user"));
            context.put("project", request.session().attribute("project"));



            String sql = String.format("SELECT * FROM theme t JOIN project p ON t.id_theme=p.id_theme WHERE p.projectID= %s", project.get("projectID"));
            DBRow theme = conn.executeQuery(sql).first();
            context.put("theme", theme);


            sql = String.format("SELECT * FROM donation_option WHERE projectID = '%s'", request.params(":idProjeto"));
            DBRowList donationOptions = conn.executeQuery(sql);
            context.put("donationOptions", donationOptions);

            return engine.render(context, "donativo.html");

        });

        //  Página para finalizar uma doação em particular
        get("/donativo/:idProjeto/:idOpcao", (request, response) -> {

            FreemarkerContext context = new FreemarkerContext();
            context.put("user", request.session().attribute("user"));
            context.put("project", request.session().attribute("project"));

            String sql = String.format("SELECT * FROM donation_option WHERE idDonationOption = '%s'", request.params(":idOpcao"));
            DBRow donationOption = conn.executeQuery(sql).first();

            context.put("donationOption", donationOption);
            request.session().attribute("donationOption", donationOption);

            if (request.session().attribute("user") == null) {
                response.redirect("/login/");
                return "";
            }

            return engine.render(context, "fazerDonativo.html");

        });

        // Leitura de dados para finalizar Donativo
        post("/donativo/:idProjeto/:idOpcao", (request, response) -> {
            FreemarkerContext context = new FreemarkerContext();
            context.put("user", request.session().attribute("user"));
            context.put("project", request.session().attribute("project"));
            context.put("donationOption", request.session().attribute("donationOption"));

            DBRow donationOption = request.session().attribute("donationOption");
            DBRow user = request.session().attribute("user");
            DBRow project = request.session().attribute("project");

            int idProjeto = Integer.parseInt(request.params(":idProjeto"));
            int idOpcao = Integer.parseInt(request.params(":idOpcao"));

            String first_name = request.queryParams("first_name");
            String last_name = request.queryParams("last_name");
            String address = request.queryParams("address");
            String cardNumber = request.queryParams("cardNumber");
            String cvv = request.queryParams("cvv");
            String expiry = request.queryParams("expiry");
            String message = request.queryParams("message");

            //Comprovativo de doação
            UUID uuid = UUID.randomUUID();
            String uuidAsString = uuid.toString();

            File f = new File("src\\resources\\comprovativos\\" + uuidAsString + ".txt");
            FileWriter fw = new FileWriter(f);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter print = new PrintWriter(bw);

            print.println("************ Comprovativo de doação ************");
            print.println("\nNome: " + (String) user.get("first_name") + " " + (String) user.get("last_name"));
            print.println("\nProjeto: " + project.get("title"));
            print.println("\nDescrição: " + donationOption.get("description"));
            print.println("\nMensagem: " + message);
            print.println("\nQuantia doada: " + donationOption.get("amount") + "$");
            print.println("\nData da doação: " + LocalDate.now().toString());

            print.close();

            String sql = String.format(Locale.ROOT, "INSERT INTO donation VALUES (default, %f, '%s', '%s', %s, %s, '%s')",
                    donationOption.get("amount"), LocalDate.now().toString(), message, user.get("userID"), idProjeto, uuidAsString);

            if (conn.executeUpdate(sql) != -1) {
                context.put("donation_is_done", true);

                sql = String.format(Locale.ROOT, "SELECT * FROM donation WHERE uuid='%s'", uuidAsString);
                DBRow donation = conn.executeQuery(sql).first();

                context.put("donation", donation);
            } else {
                context.put("donation_error", true);
            }

            return engine.render(context, "fazerDonativo.html");

        });

    }
}
