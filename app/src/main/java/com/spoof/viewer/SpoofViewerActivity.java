package com.spoof.viewer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Tela (aberta pela entrada injetada no Settings) que lista jogo -> perfil e os
 * campos que cada perfil aplica, lendo games_spoof.prop com PropParser.
 * UI construida em codigo, sem dependencias externas, para um build de CI simples.
 */
public class SpoofViewerActivity extends Activity {

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.app_name));

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, dp(32));
        scroll.addView(root);
        setContentView(scroll);

        PropParser.Config cfg = PropParser.parse(PropParser.DEFAULT_PATH);

        if (cfg.error != null) {
            root.addView(title("Não foi possível ler os spoofs"));
            root.addView(body(cfg.error));
            root.addView(body("Confira se o arquivo existe e está com permissão de leitura (0644)."));
            return;
        }

        // Cabecalho com contagem
        TextView head = new TextView(this);
        head.setText(cfg.gameCount() + " jogos  •  " + cfg.usedProfileCount() + " perfis em uso");
        head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        head.setPadding(0, 0, 0, dp(4));
        head.setAlpha(0.7f);
        root.addView(head);

        TextView path = new TextView(this);
        path.setText(PropParser.DEFAULT_PATH);
        path.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        path.setAlpha(0.5f);
        path.setPadding(0, 0, 0, dp(12));
        root.addView(path);

        // Um cartao por perfil que tem jogos (na ordem de definicao do arquivo)
        for (PropParser.Profile p : cfg.profiles.values()) {
            if (p.games.isEmpty()) continue;
            root.addView(profileCard(p));
        }

        // Nota sobre perfis definidos mas nao usados
        if (!cfg.unusedProfiles.isEmpty()) {
            TextView note = body("Perfis definidos mas sem jogo apontando para eles: "
                    + TextUtils.join(", ", cfg.unusedProfiles));
            note.setPadding(0, dp(8), 0, 0);
            note.setAlpha(0.6f);
            root.addView(note);
        }
    }

    private View profileCard(PropParser.Profile p) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int cp = dp(14);
        card.setPadding(cp, cp, cp, cp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        card.setLayoutParams(lp);
        card.setBackgroundColor(Color.argb(28, 128, 128, 128));

        // Nome do perfil (bonito) + contagem de spoofs, no accent Monet
        TextView name = new TextView(this);
        name.setText(p.displayName() + " - Spoofs: " + p.games.size());
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        name.setTypeface(name.getTypeface(), android.graphics.Typeface.BOLD);
        try { name.setTextColor(getColor(R.color.accent)); } catch (Throwable ignored) {}
        card.addView(name);

        // Campos aplicados
        StringBuilder fb = new StringBuilder();
        for (PropParser.Field f : p.fields) {
            if (fb.length() > 0) fb.append('\n');
            fb.append(f.target).append(" = ").append(f.value);
        }
        TextView fields = new TextView(this);
        fields.setText(fb.length() == 0 ? "(nenhum campo ativo)" : fb.toString());
        fields.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        fields.setPadding(0, dp(6), 0, dp(8));
        fields.setAlpha(0.85f);
        card.addView(fields);

        // Linha divisoria
        View div = new View(this);
        div.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        div.setBackgroundColor(Color.argb(40, 128, 128, 128));
        card.addView(div);

        // Lista de jogos
        for (PropParser.Game g : p.games) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(6), 0, dp(2));

            TextView gl = new TextView(this);
            gl.setText(g.label.isEmpty() ? g.pkg : g.label);
            gl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            row.addView(gl);

            if (!g.label.isEmpty()) {
                TextView gp = new TextView(this);
                gp.setText(g.pkg);
                gp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                gp.setAlpha(0.55f);
                row.addView(gp);
            }
            card.addView(row);
        }
        return card;
    }

    private TextView title(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setPadding(0, 0, 0, dp(8));
        return t;
    }

    private TextView body(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setGravity(Gravity.START);
        t.setPadding(0, dp(2), 0, dp(2));
        return t;
    }
}
