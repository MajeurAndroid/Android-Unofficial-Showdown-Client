package com.majeur.psclient.util;

import android.os.AsyncTask;
import android.util.Log;

import com.majeur.psclient.model.BattleFormat;
import com.majeur.psclient.model.DexPokemon;
import com.majeur.psclient.model.Stats;
import com.majeur.psclient.model.Team;
import com.majeur.psclient.model.TeamPokemon;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.majeur.psclient.model.Id.toId;

public class ShowdownTeamParser {

    public interface DexPokemonFactory {
        public DexPokemon loadDexPokemon(String name);
    }

    @SuppressWarnings("StaticFieldLeak")
    public static void parseTeams(final String importString, final DexPokemonFactory factory,
                           final Callback<List<Team>> callback) {
        new AsyncTask<Object, Object, List<Team>>() {
            @Override
            protected List<Team> doInBackground(Object[] unused) {
                return parseTeams(importString, factory);
            }

            @Override
            protected void onPostExecute(List<Team> teams) {
                callback.callback(teams);
            }
        }.execute();
    }

    public static List<Team> parseTeams(String importString, DexPokemonFactory dexPokemonFactory) {
        Pattern pattern = Pattern.compile("(?<====).*?(?====)");
        Matcher matcher = pattern.matcher(importString);
        Queue<String> teamHeaders = new LinkedList<>();
        while (matcher.find())
            teamHeaders.add(matcher.group().trim());

        String[] teamsStrings = importString.split("\\===.*?\\===");
        if (teamsStrings.length == 0) {
            return null;
        }

        List<Team> teams = new LinkedList<>();
        for (String teamString : teamsStrings) {
            teamString = teamString.trim();
            if (teamString.length() < 5) // Filtering empty strings
                continue;

            String teamHeader = teamHeaders.poll();

            if (teamHeader != null) {
                String format = getFormatFromHeader(teamHeader);
                String teamLabel = getLabelFromHeader(teamHeader);
                teams.add(parseTeam(teamString, format, teamLabel, dexPokemonFactory));
            } else {
                teams.add(parseTeam(teamString,
                        BattleFormat.FORMAT_OTHER.id(),
                        "Unnamed team",
                        dexPokemonFactory));
            }
        }
        return teams;
    }

    private static String getFormatFromHeader(String teamHeader) {
        int startIndex = teamHeader.indexOf('[');
        int endIndex = teamHeader.indexOf(']');
        if (startIndex < 0 || endIndex < 0)
            return null;
        return teamHeader.substring(startIndex + 1, endIndex);
    }

    private static String getLabelFromHeader(String teamHeader) {
        int startIndex = teamHeader.indexOf(']');
        if (startIndex < 0)
            return teamHeader.trim();
        return teamHeader.substring(startIndex + 1).trim();
    }

    private static Team parseTeam(String importString, String format, String label, DexPokemonFactory dexPokemonFactory) {
        importString = importString.replace("\r", ""); // Make sure there is no CR char when we split with LF
        String[] teamStrings = importString.split("\n\n");
        if (teamStrings.length == 0) {
            return null;
        }

        List<TeamPokemon> pokemons = new LinkedList<>();
        for (String pokemonString : teamStrings) {
            TeamPokemon parsedPokemon = parsePokemon(pokemonString.trim(), dexPokemonFactory);
            if (parsedPokemon != null)
                pokemons.add(parsedPokemon);
        }

        if (label != null && label.contains("[") && label.contains("]")) {
            int charIndex = label.indexOf(']');
            return new Team(label.substring(charIndex + 1), pokemons, label.substring(1, charIndex));
        }
        return new Team(label, pokemons, format);
    }

    public static TeamPokemon parsePokemon(String importString, DexPokemonFactory dexPokemonFactory) {
        String[] pokemonStrings = importString.trim().split("\n");
        if (pokemonStrings.length == 0) {
            return null;
        }

        Log.e("PSP curr", Arrays.toString(pokemonStrings));
        String pokemonMainData = pokemonStrings[0]; // split 0 is Name @ Item or Name or nickname (Name) or  nickname (Name) @ Item
        String pokemonName = "", pokemonNickname = null, pokemonItem = null, pokemonGender = null;
        TeamPokemon p = null;
        boolean isGender = false; // no nickname, but gender
        if (pokemonMainData.contains("@")) {
            String[] nameItem = pokemonMainData.split("@");
            pokemonItem = nameItem[1];
            pokemonMainData = nameItem[0];
        }

        if (pokemonMainData.contains("(") && pokemonMainData.contains(")")) {
            int countOpen = pokemonMainData.length() - pokemonMainData.replace("(", "").length();
            int countClosed = pokemonMainData.length() - pokemonMainData.replace(")", "").length();

            if (countOpen == 1 && countClosed == 1) {
                // either name or gender
                String genderOrName = pokemonMainData.substring(pokemonMainData.lastIndexOf("(") + 1, pokemonMainData.lastIndexOf(")"));

                if (genderOrName.equals("M") || genderOrName.equals("F") || genderOrName.equals("N")) {
                    pokemonGender = genderOrName;
                    pokemonName = pokemonMainData.substring(0, pokemonMainData.lastIndexOf("("));
                } else {
                    pokemonName = genderOrName;
                    pokemonNickname = pokemonMainData.substring(0, pokemonMainData.lastIndexOf("("));
                }
            } else {
                // both name + gender
                String genderOrName = pokemonMainData.substring(pokemonMainData.lastIndexOf("(") + 1, pokemonMainData.lastIndexOf(")"));

                if (genderOrName.equals("M") || genderOrName.equals("F") || genderOrName.equals("N")) {
                    pokemonGender = genderOrName;
                    pokemonMainData = pokemonMainData.substring(0, pokemonMainData.lastIndexOf("("));
                    pokemonName = pokemonMainData.substring(pokemonMainData.lastIndexOf("(") + 1, pokemonMainData.lastIndexOf(")"));
                    pokemonNickname = pokemonMainData.substring(0, pokemonMainData.lastIndexOf("("));
                } else {
                    // is nickname with ()()() and (name)
                    pokemonName = genderOrName;
                    pokemonNickname = pokemonMainData.substring(0, pokemonMainData.lastIndexOf("("));
                }
            }
        } else {
            pokemonName = pokemonMainData;
        }

        p = new TeamPokemon(pokemonName);

        if (pokemonNickname != null) {
            p.name = pokemonNickname.trim();
        }

        if (pokemonItem != null) {
            p.item = toId(pokemonItem);
        }

        if (pokemonGender != null) {
            if (pokemonGender.equals("M") || pokemonGender.equals("F") || pokemonGender.equals("N")) {
                p.gender = pokemonGender;
            }
        }

        for (int i = 1; i < pokemonStrings.length; i++) {
            String currentString = pokemonStrings[i];
            if (currentString.startsWith("-")) {
                // its a move!
                // same as items, it's a real name , we need an id. Lowercasing and removing spaces  + - should do the trick
                String move = currentString.substring(currentString.indexOf("-") + 1);
                move = toId(move);
                if (p.moves == null)
                    p.moves = new String[]{move};
                else
                    p.moves = Utils.append(p.moves, move);

            } else if (currentString.startsWith("IVs:")) {
                String ivs = currentString.substring(currentString.indexOf(":") + 1);
                String[] ivsSplit = ivs.split("/");

                for (String iv : ivsSplit) {
                    iv = iv.trim();
                    String value, stat;
                    String[] valueStat = iv.split(" ");
                    stat = valueStat[1];
                    value = valueStat[0];
                    int ivValue;
                    try {
                        ivValue = Integer.parseInt(value.trim());
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    switch (stat) {
                        case "HP":
                            p.ivs.hp = ivValue;
                            break;
                        case "Atk":
                            p.ivs.atk = ivValue;
                            break;
                        case "Def":
                            p.ivs.def = ivValue;
                            break;
                        case "SpA":
                            p.ivs.spa = ivValue;
                            break;
                        case "SpD":
                            p.ivs.spd = ivValue;
                            break;
                        case "Spe":
                            p.ivs.spe = ivValue;
                            break;
                    }
                }
            } else if (currentString.startsWith("EVs:")) {
                String evs = currentString.substring(currentString.indexOf(":") + 1);
                String[] evssplit = evs.split("/");

                for (String ev : evssplit) {
                    ev = ev.trim();
                    String value, stat;
                    String[] valueStat = ev.split(" ");
                    stat = valueStat[1];
                    value = valueStat[0];
                    int evValue;
                    try {
                        evValue = Integer.parseInt(value.trim());
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    switch (stat) {
                        case "HP":
                            p.evs.hp = evValue;
                            break;
                        case "Atk":
                            p.evs.atk = evValue;
                            break;
                        case "Def":
                            p.evs.def = evValue;
                            break;
                        case "SpA":
                            p.evs.spa = evValue;
                            break;
                        case "SpD":
                            p.evs.spd = evValue;
                            break;
                        case "Spe":
                            p.evs.spe = evValue;
                            break;
                    }
                }
            } else if (currentString.contains("Nature")) { /* Example : Adamant Nature*/
                String nature = currentString.substring(0, currentString.indexOf("Nature")).trim();
                p.nature = nature;
            } else if (currentString.startsWith("Ability:")) {
                String abilityName = currentString.substring(currentString.indexOf(":") + 1).trim();
                DexPokemon dexPokemon = dexPokemonFactory.loadDexPokemon(toId(p.species));
                if (dexPokemon != null) {
                    for (int j = 0; j < dexPokemon.abilities.size(); j++) {
                        String ability = dexPokemon.abilities.get(j);
                        if (ability.equals(abilityName)) {
                            p.ability = abilityName;
                            break;
                        }
                    }

                    if (abilityName.equals(dexPokemon.hiddenAbility)) {
                        p.ability = abilityName;
                    }
                }
            } else if (currentString.startsWith("Level:")) {
                String level = currentString.substring(currentString.indexOf(":") + 1).trim();
                try {
                    p.level = Integer.parseInt(level);
                } catch (NumberFormatException e) {
                    break;
                }
            } else if (currentString.startsWith("Shiny")) {
                p.shiny = true;
            }
        }

        Log.e("PSP", "Parsed " + pokemonName);
        Log.e("PSP", p.toString());
        // If p.moves is null, the pokemon parsed is probably broken.
        return p.moves != null ? p : null;
    }

    public static String fromPokemon(TeamPokemon curSet) {
        String text = "";
        if (curSet.name != null && !curSet.name.equals(curSet.species)) {
            text += "" + curSet.name + " (" + curSet.species + ")";
        } else {
            text += "" + curSet.species;
        }
        if ("m".equalsIgnoreCase(curSet.gender)) text += " (M)";
        if ("f".equalsIgnoreCase(curSet.gender)) text += " (F)";
        if (curSet.item != null && curSet.item.length() > 0) {
            text += " @ " + curSet.item;
        }
        text += "  \n";
        if (curSet.ability != null) {
            text += "Ability: " + curSet.ability + "  \n";
        }
        if (curSet.level != 100) {
            text += "Level: " + curSet.level + "  \n";
        }
        if (curSet.shiny) {
            text += "Shiny: Yes  \n";
        }
        if (curSet.happiness != 255) {
            text += "Happiness: " + curSet.happiness + "  \n";
        }
        if (curSet.pokeball != null) {
            text += "Pokeball: " + curSet.pokeball + "  \n";
        }
        if (curSet.hpType != null) {
            text += "Hidden Power: " + curSet.hpType + "  \n";
        }
        boolean first = true;
        if (curSet.evs != null) {
            for (int i = 0; i < 6; i++) {
                if (curSet.evs.get(i) == 0) continue;
                if (first) {
                    text += "EVs: ";
                    first = false;
                } else {
                    text += " / ";
                }
                text += curSet.evs.get(i) + " " + Stats.getName(i);
            }
        }
        if (!first) {
            text += "  \n";
        }
        if (curSet.nature != null) {
            text += curSet.nature + " Nature" + "  \n";
        }
        first = true;
        if (curSet.ivs != null) {
            boolean defaultIvs = true;
            String hpType = null;
            Stats dummyStats = new Stats(0);
            for (int j = 0; j < curSet.moves.length; j++) {
                String move = curSet.moves[j];
                if (move != null && move.length() >= 13 && move.substring(0, 13).equals("Hidden Power ")
                        && !move.substring(0, 14).equals("Hidden Power [")) {
                    hpType = move.substring(13);
                    if (!Stats.checkHpType(hpType)) {
                        continue;
                    }
                    for (int i = 0; i < 6; i++) {
                        dummyStats.setForHpType(hpType);
                        if (curSet.ivs.get(i) != dummyStats.get(i)) {
                            defaultIvs = false;
                            break;
                        }
                    }
                }
            }
            if (defaultIvs && hpType == null) {
                for (int i = 0; i < 6; i++) {
                    if (curSet.ivs.get(i) != 31) {
                        defaultIvs = false;
                        break;
                    }
                }
            }
            if (!defaultIvs) {
                for (int i = 0; i < 6; i++) {
                    if (curSet.ivs.get(i) == 31) continue;
                    if (first) {
                        text += "IVs: ";
                        first = false;
                    } else {
                        text += " / ";
                    }
                    text += curSet.ivs.get(i) + " " + Stats.getName(i);
                }
            }
        }
        if (!first) {
            text += "  \n";
        }
        if (curSet.moves != null && curSet.moves.length > 0)
            for (int j = 0; j < curSet.moves.length; j++) {
                String move = curSet.moves[j];
                if (move == null) continue;
                if (move.length() >= 13 && move.substring(0, 13).equalsIgnoreCase("Hidden Power ")) {
                    move = move.substring(0, 13) + '[' + move.substring(13) + ']';
                }
                text += "- " + move + "  \n";
            }
        return text;
    }

}
