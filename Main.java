import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    static Map<String,String> usuarios = new ConcurrentHashMap<>();
    static Map<String,LoginState> estados = new ConcurrentHashMap<>();
    static Map<String,IpState> ips = new ConcurrentHashMap<>();

    static int limite = 5;
    static long tempo = 30000;
    static long max = 900000;

    static {
        usuarios.put("admin","Senha123");
        usuarios.put("usuario","Teste456");
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        String ip = "127.0.0.1";

        while(true) {

            System.out.print("Usuario (sair para fechar): ");
            String u = sc.nextLine();

            if(u.equalsIgnoreCase("sair")) break;

            System.out.print("Senha: ");
            String s = sc.nextLine();

            System.out.println(login(u,s,ip));

        }

        sc.close();
    }

    public static String login(String u,String s,String ip) {

        u = u.trim().toLowerCase();

        IpState i = ips.computeIfAbsent(ip,k -> new IpState());

        long b = i.tentativa();

        if(b > 0) return "IP bloqueado por " + b + " segundos";

        LoginState l = estados.computeIfAbsent(u,k -> new LoginState());

        synchronized(l) {

            long agora = System.currentTimeMillis();

            if(agora < l.bloqueado) {
                return "Conta bloqueada. Aguarde " +
                        ((l.bloqueado-agora+999)/1000) + " segundos";
            }

            if(usuarios.containsKey(u) && usuarios.get(u).equals(s)) {

                l.erros = 0;
                l.nivel = 0;
                l.bloqueado = 0;

                return "Login autorizado";
            }

            l.erros++;

            if(l.erros >= limite) {

                l.nivel++;

                long t = Math.min(tempo * (1L << Math.min(l.nivel-1,5)),max);

                l.bloqueado = agora + t;
                l.erros = 0;

                return "Muitas tentativas. Bloqueado por " + t/1000 + " segundos";
            }

            return "Senha incorreta. Tentativas restantes: " + (limite-l.erros);
        }
    }

    static class LoginState {
        int erros = 0;
        int nivel = 0;
        long bloqueado = 0;
    }

    static class IpState {

        int erros = 0;
        long inicio = System.currentTimeMillis();
        long bloqueado = 0;

        synchronized long tentativa() {

            long agora = System.currentTimeMillis();

            if(agora < bloqueado)
                return (bloqueado-agora+999)/1000;

            if(agora-inicio >= 60000) {
                inicio = agora;
                erros = 0;
            }

            erros++;

            if(erros > 20) {
                bloqueado = agora + 60000;
                return 60;
            }

            return 0;
        }
    }
}