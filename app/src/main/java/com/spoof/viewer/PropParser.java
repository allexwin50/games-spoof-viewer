package com.spoof.viewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Le e interpreta /product/etc/assets/games_spoof.prop usando EXATAMENTE as
 * mesmas regras do GamesPropsConfig injetado no framework (secoes [map] e
 * [profile.x], comentarios com '#', normalizacao de chave e minusculas), para
 * que a tela mostre o mesmo que o framework de fato aplica. Campos comentados
 * no .prop nao aparecem, porque o framework tambem nao os aplica.
 *
 * Diferenca proposital: o nome do pacote dos jogos e' exibido com a grafia
 * original do arquivo (apenas para leitura humana); o casamento em si e'
 * insensivel a maiusculas/minusculas, acompanhando a correcao do apply().
 */
public final class PropParser {

    public static final String DEFAULT_PATH = "/product/etc/assets/games_spoof.prop";

    /** Campos que o framework aplica, na ordem do apply(), com o alvo em Build.*. */
    static final String[][] FIELDS = {
            {"manufacturer", "Build.MANUFACTURER"},
            {"brand", "Build.BRAND"},
            {"device", "Build.DEVICE"},
            {"model", "Build.MODEL"},
            {"name", "Build.PRODUCT"},
            {"product", "Build.PRODUCT"},
            {"board", "Build.BOARD"},
            {"fingerprint", "Build.FINGERPRINT"},
            {"id", "Build.ID"},
            {"build_id", "Build.ID"},
            {"type", "Build.TYPE"},
            {"tags", "Build.TAGS"},
            {"security_patch", "VERSION.SECURITY_PATCH"},
            {"release", "VERSION.RELEASE"},
            {"incremental", "VERSION.INCREMENTAL"},
            {"device_initial_sdk_int", "VERSION.DEVICE_INITIAL_SDK_INT"},
    };

    public static final class Field {
        public final String key;     // chave no .prop (ex.: model)
        public final String target;  // alvo no app (ex.: Build.MODEL)
        public final String value;   // valor aplicado
        Field(String key, String target, String value) {
            this.key = key; this.target = target; this.value = value;
        }
    }

    public static final class Profile {
        public final String name;
        public final List<Field> fields = new ArrayList<>();
        public final List<Game> games = new ArrayList<>();
        Profile(String name) { this.name = name; }
    }

    public static final class Game {
        public final String pkg;    // grafia original do pacote
        public final String label;  // rotulo amigavel (comentario acima da linha)
        public final String profileName;
        Game(String pkg, String label, String profileName) {
            this.pkg = pkg; this.label = label; this.profileName = profileName;
        }
    }

    public static final class Config {
        public final List<Game> games = new ArrayList<>();
        public final LinkedHashMap<String, Profile> profiles = new LinkedHashMap<>();
        public final List<String> unusedProfiles = new ArrayList<>();
        public String error; // nao-nulo se o arquivo nao pode ser lido
        public int gameCount() { return games.size(); }
        public int usedProfileCount() {
            Set<String> s = new LinkedHashSet<>();
            for (Game g : games) s.add(g.profileName);
            return s.size();
        }
    }

    private PropParser() {}

    static String normKey(String k) {
        String[] pre = {"ro.product.", "ro.build.version.", "ro.build."};
        for (String p : pre) if (k.startsWith(p)) return k.substring(p.length());
        return k;
    }

    public static Config parse(String path) {
        Config cfg = new Config();
        File f = new File(path);
        if (!f.isFile()) { cfg.error = "Arquivo nao encontrado:\n" + path; return cfg; }

        // Primeiro passe: le o arquivo cru em secoes, preservando ordem e grafia.
        // profileRaw: nome do perfil -> (chaveNormalizadaMinuscula -> valor)
        LinkedHashMap<String, LinkedHashMap<String, String>> profileRaw = new LinkedHashMap<>();
        // mapRaw: lista de {pkgOriginal, label, profileValue} na ordem do arquivo
        List<String[]> mapRaw = new ArrayList<>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            String section = "";
            String lastComment = null;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() > 0 && line.charAt(0) == '﻿') line = line.substring(1);
                line = line.trim();
                if (line.isEmpty()) continue;
                char c = line.charAt(0);
                if (c == '#') { lastComment = line.substring(1).trim(); continue; }
                if (c == '[') {
                    if (line.endsWith("]")) section = line.substring(1, line.length() - 1).trim();
                    lastComment = null;
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) { lastComment = null; continue; }
                String rawKey = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();

                if (section.equals("map")) {
                    // pacote preserva grafia original; casamento e' por minuscula
                    mapRaw.add(new String[]{rawKey, lastComment == null ? "" : lastComment, val});
                } else if (section.startsWith("profile.")) {
                    String prof = section.substring("profile.".length());
                    String key = normKey(rawKey).toLowerCase();
                    profileRaw.computeIfAbsent(prof, k -> new LinkedHashMap<>()).put(key, val);
                }
                lastComment = null;
            }
        } catch (Throwable t) {
            cfg.error = "Falha ao ler o arquivo:\n" + t;
            return cfg;
        } finally {
            if (br != null) try { br.close(); } catch (Throwable ignored) {}
        }

        // Monta os perfis definidos, com os campos na ordem que o framework aplica.
        for (Map.Entry<String, LinkedHashMap<String, String>> e : profileRaw.entrySet()) {
            Profile p = new Profile(e.getKey());
            LinkedHashMap<String, String> kv = e.getValue();
            for (String[] fd : FIELDS) {
                String v = kv.get(fd[0]);
                if (v != null && !v.isEmpty()) p.fields.add(new Field(fd[0], fd[1], v));
            }
            cfg.profiles.put(e.getKey(), p);
        }

        // Associa jogos aos perfis (casamento de perfil por grafia exata do valor,
        // que no arquivo coincide com o nome da secao [profile.x]).
        for (String[] m : mapRaw) {
            String pkg = m[0];
            String label = m[1];
            String profName = m[2];
            Game g = new Game(pkg, label, profName);
            cfg.games.add(g);
            Profile p = cfg.profiles.get(profName);
            if (p != null) p.games.add(g);
        }

        // Perfis definidos mas sem nenhum jogo apontando para eles.
        for (Profile p : cfg.profiles.values()) {
            if (p.games.isEmpty()) cfg.unusedProfiles.add(p.name);
        }
        return cfg;
    }
}
