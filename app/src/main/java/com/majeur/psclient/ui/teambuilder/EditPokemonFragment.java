package com.majeur.psclient.ui.teambuilder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.majeur.psclient.R;
import com.majeur.psclient.io.AllItemsLoader;
import com.majeur.psclient.io.AllSpeciesLoader;
import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.io.DexPokemonLoader;
import com.majeur.psclient.io.GlideHelper;
import com.majeur.psclient.io.LearnsetLoader;
import com.majeur.psclient.io.MoveDetailsLoader;
import com.majeur.psclient.model.BasePokemon;
import com.majeur.psclient.model.DexPokemon;
import com.majeur.psclient.model.Item;
import com.majeur.psclient.model.Move;
import com.majeur.psclient.model.Nature;
import com.majeur.psclient.model.Species;
import com.majeur.psclient.model.Stats;
import com.majeur.psclient.model.TeamPokemon;
import com.majeur.psclient.model.Type;
import com.majeur.psclient.util.CategoryDrawable;
import com.majeur.psclient.util.FilterableAdapter;
import com.majeur.psclient.util.RangeNumberTextWatcher;
import com.majeur.psclient.util.ShowdownTeamParser;
import com.majeur.psclient.util.SimpleOnItemSelectedListener;
import com.majeur.psclient.util.SimpleTextWatcher;
import com.majeur.psclient.util.Utils;
import com.majeur.psclient.widget.StatsTable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;
import static android.content.Context.CLIPBOARD_SERVICE;
import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.util.Utils.alphaColor;
import static com.majeur.psclient.util.Utils.array;
import static com.majeur.psclient.util.Utils.dpToPx;
import static com.majeur.psclient.util.Utils.parseInt;
import static com.majeur.psclient.util.Utils.str;

@SuppressWarnings({"unchecked", "rawtypes"})
public class EditPokemonFragment extends Fragment {

    private static final String ARG_SLOT_INDEX = "arg-slot-index";
    private static final String ARG_PKMN = "arg-pkmn";

    public static EditPokemonFragment create(int slotIndex, TeamPokemon pkmn) {
        EditPokemonFragment fragment = new EditPokemonFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SLOT_INDEX, slotIndex);
        args.putSerializable(ARG_PKMN, pkmn);
        fragment.setArguments(args);
        return fragment;
    }

    private boolean mAttachedToActivity;
    private int mTextHighlightColor;

    // Helpers
    private AllSpeciesLoader mSpeciesLoader;
    private AllItemsLoader mItemsLoader;
    private DexPokemonLoader mDexPokemonLoader;
    private LearnsetLoader mLearnsetLoader;
    private MoveDetailsLoader mMoveDetailsLoader;
    private GlideHelper mGlideHelper;
    private DexIconLoader mDexIconLoader;

    // Views
    private AutoCompleteTextView mSpeciesTextView;
    private MaterialButton mClearButton;
    private ImageView mSpriteImageView;
    private EditText mNameTextView;
    private EditText mLevelTextView;
    private EditText mHappinessTextView;
//    private TextView mGenderTextView;
    private CheckBox mShinyCheckbox;
    private Spinner mAbilitySpinner;
    private AutoCompleteTextView mItemTextView;
    private AutoCompleteTextView[] mMoveTextViews;
    private StatsTable mStatsTable;
    private Spinner mNatureSpinner;
    private Spinner mHpTypeSpinner;
    private View mExportButton;

    // Data
    private int mSlotIndex;
    private Species mCurrentSpecies;
    private Item mCurrentItem;
    private Stats mCurrentBaseStats;
    private Stats mCurrentEvs;
    private Stats mCurrentIvs;
    private String mCurrentAbility;
    private String[] mCurrentMoves;
    private Nature mCurrentNature;

    private boolean mHasPokemonData;

    private final View.OnFocusChangeListener mACETFocusListener = (view, hasFocus) -> {
        AutoCompleteTextView textView = (AutoCompleteTextView) view;
        if (hasFocus && textView.length() == 0) textView.showDropDown();
        if (!hasFocus && getActivity() != null)
            Utils.hideSoftInputMethod(getActivity());
    };

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mAttachedToActivity = true;
        mTextHighlightColor = alphaColor(ContextCompat.getColor(context, R.color.secondary), 0.45f);
        mSlotIndex = getArguments().getInt(ARG_SLOT_INDEX);
        EditTeamActivity activity = (EditTeamActivity) context;
        mSpeciesLoader = activity.getSpeciesLoader();
        mItemsLoader = activity.getItemsLoader();
        mDexPokemonLoader = activity.getDexPokemonLoader();
        mLearnsetLoader = activity.getLearnsetLoader();
        mMoveDetailsLoader = activity.getMoveDetailsLoader();
        mGlideHelper = activity.getGlideHelper();
        mDexIconLoader = activity.getDexIconLoader();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentEvs = new Stats(0);
        mCurrentIvs = new Stats(31);
        mCurrentMoves = new String[4];
        mMoveTextViews = new AutoCompleteTextView[4];
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_pokemon, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSpeciesTextView = view.findViewById(R.id.speciesTextView);
        mSpriteImageView = view.findViewById(R.id.spriteImageView);
        mNameTextView = view.findViewById(R.id.nameEditText);
        mLevelTextView = view.findViewById(R.id.levelEditText);
        mHappinessTextView = view.findViewById(R.id.happinessEditText);
//        mGenderTextView = view.findViewById(R.id.genderTextView);
        mShinyCheckbox = view.findViewById(R.id.shinyCheckBox);
        mAbilitySpinner = view.findViewById(R.id.abilityTextView);
        mItemTextView = view.findViewById(R.id.itemTextView);
        mMoveTextViews[0] = view.findViewById(R.id.move1TextView);
        mMoveTextViews[1] = view.findViewById(R.id.move2TextView);
        mMoveTextViews[2] = view.findViewById(R.id.move3TextView);
        mMoveTextViews[3] = view.findViewById(R.id.move4TextView);
        mStatsTable = view.findViewById(R.id.statsTable);
        mNatureSpinner = view.findViewById(R.id.natureSpinner);
        mHpTypeSpinner = view.findViewById(R.id.hpTypeSpinner);
        mClearButton = view.findViewById(R.id.clearPokemon);

        mSpeciesLoader.load(array(""), results -> {
            if (!mAttachedToActivity) return;
            mSpeciesTextView.setAdapter(new SpeciesAdapter(mDexIconLoader, results[0], mTextHighlightColor));
        });
        mSpeciesTextView.setThreshold(1);
        mSpeciesTextView.setDropDownWidth(dpToPx(196));
        mSpeciesTextView.setOnItemClickListener((adapterView, view1, i, l) -> {
            Adapter adapter = adapterView.getAdapter();
            Species newSpecies = (Species) adapter.getItem(i);
            trySpecies(newSpecies.id);
        });

        mNameTextView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String input = editable.toString();
                String regex = "[,|\\[\\]]"; // escape |,[] characters
                if (input.matches(".*" + regex + ".*")) {
                    editable.clear();
                    editable.append(input.replaceAll(regex, ""));
                } else {
                    notifyPokemonDataChanged();
                }
            }
        });

        mLevelTextView.addTextChangedListener(new RangeNumberTextWatcher(1, 100));
        mLevelTextView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                mStatsTable.setLevel(getCurrentLevel());
                notifyPokemonDataChanged();
            }
        });
        mLevelTextView.setOnFocusChangeListener((view12, hasFocus) -> {
            if (!hasFocus && mLevelTextView.length() == 0)
                mLevelTextView.setText("100");
        });

        mShinyCheckbox.setOnCheckedChangeListener((compoundButton, b) -> {
            updatePokemonSprite();
            notifyPokemonDataChanged();
        });

        mHappinessTextView.addTextChangedListener(new RangeNumberTextWatcher(0, 255));
        mHappinessTextView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                notifyPokemonDataChanged();
            }
        });
        mHappinessTextView.setOnFocusChangeListener((view13, hasFocus) -> {
            if (!hasFocus && mHappinessTextView.length() == 0)
                mHappinessTextView.setText("255");
        });

        mAbilitySpinner.setOnItemSelectedListener(new SimpleOnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Adapter adapter = adapterView.getAdapter();
                String readableAbility = (String) adapter.getItem(i);
                mCurrentAbility = toId(readableAbility.replace(" (Hidden)", ""));
                notifyPokemonDataChanged();
            }
        });

        mItemsLoader.load(array(""), results -> {
            List<Item> list = results[0];
            list.size();
            if (!mAttachedToActivity) return;
            mItemTextView.setAdapter(new FilterableAdapter<>(results[0], mTextHighlightColor));
        });
        mItemTextView.setOnItemClickListener((adapterView, view14, i, l) -> {
            Adapter adapter = adapterView.getAdapter();
            mCurrentItem = (Item) adapter.getItem(i);
            notifyPokemonDataChanged();
        });
        mItemTextView.setOnFocusChangeListener(mACETFocusListener);

        for (int i = 0; i < 4; i++) {
            final int index = i;
            final AutoCompleteTextView textView = mMoveTextViews[i];
            textView.setThreshold(1);
            textView.setOnFocusChangeListener(mACETFocusListener);
            textView.setOnItemClickListener((adapterView, view15, i1, l) -> {
                Adapter adapter = adapterView.getAdapter();
                mCurrentMoves[index] = (String) adapter.getItem(i1);
                notifyPokemonDataChanged();
                /* Check if we have the full name to display to user */
                if (view15.getTag() instanceof MovesAdapter.ViewHolder) {
                    CharSequence text = ((MovesAdapter.ViewHolder) view15.getTag()).mNameView.getText();
                    if (text.length() > 0 && Character.isUpperCase(text.charAt(0)))
                        textView.setText(text.toString()); // Prevents highlight spans
                }
                if (index < 3) {
                    if (mMoveTextViews[index+1].length() == 0)
                        mMoveTextViews[index+1].requestFocus();
                    else
                        textView.clearFocus();
                } else {
                    textView.clearFocus();
                }
            });
        }

        mStatsTable.setRowClickListener((statsTable, rowName, index) -> {
            if (mCurrentBaseStats == null || mCurrentIvs == null || mCurrentEvs == null ||
                mCurrentNature == null) return;
            EditStatDialog dialog = EditStatDialog.newInstance(rowName, mCurrentBaseStats.get(index),
                    mCurrentEvs.get(index), mCurrentIvs.get(index), getCurrentLevel(),
                    mCurrentNature.getStatModifier(index), mCurrentEvs.sum());
            dialog.setTargetFragment(EditPokemonFragment.this, 0);
            //noinspection ConstantConditions
            dialog.show(getFragmentManager(), "");
        });

        mCurrentNature = Nature.DEFAULT;
        mNatureSpinner.setAdapter(new ArrayAdapter<>(view.getContext(),
                android.R.layout.simple_dropdown_item_1line,
                Nature.ALL));
        mNatureSpinner.setOnItemSelectedListener(new SimpleOnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ArrayAdapter<Nature> adapter = (ArrayAdapter<Nature>) adapterView.getAdapter();
                mCurrentNature = adapter.getItem(i);
                mStatsTable.setNature(mCurrentNature);
                notifyPokemonDataChanged();
            }
        });

        mHpTypeSpinner.setAdapter(new ArrayAdapter<>(view.getContext(),
                android.R.layout.simple_dropdown_item_1line,
                Type.HP_TYPES));
        mHpTypeSpinner.setOnItemSelectedListener(new SimpleOnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) adapterView.getAdapter();
                mCurrentIvs.setForHpType(adapter.getItem(i));
                mStatsTable.setIVs(mCurrentIvs);
                notifyPokemonDataChanged();
            }
        });

        mClearButton.setOnClickListener(view16 -> clearSelectedSpecies());

        mExportButton = view.findViewById(R.id.export);
        mExportButton.setOnClickListener(view17 -> {
            if (!mHasPokemonData) return;
            TeamPokemon pokemon = buildPokemon();
            String text = ShowdownTeamParser.fromPokemon(pokemon);
            Toast.makeText(getContext(), "Pokemon exported to clipboard", Toast.LENGTH_LONG).show();
            ClipboardManager clipboard = (ClipboardManager) view17.getContext().getSystemService(CLIPBOARD_SERVICE);
            if (clipboard == null) return;
            ClipData clip = ClipData.newPlainText("Exported Pokemon", text);
            clipboard.setPrimaryClip(clip);
        });

        view.findViewById(R.id.importButton).setOnClickListener(view18 -> {
            ClipboardManager clipboard = (ClipboardManager) view18.getContext().getSystemService(CLIPBOARD_SERVICE);
            if (clipboard == null) return;
            ClipData clip = clipboard.getPrimaryClip();
            if (clip == null) {
                Toast.makeText(getContext(), "There is nothing in clipboard.",
                        Toast.LENGTH_LONG).show();
            } else if (clip.getDescription().hasMimeType(MIMETYPE_TEXT_PLAIN) && clip.getItemCount() > 0) {
                final TeamPokemon pokemon = ShowdownTeamParser.parsePokemon(
                        clip.getItemAt(0).getText().toString(),
                        name -> mDexPokemonLoader.load(array(name))[0]);
                if (pokemon != null) {
                    mDexPokemonLoader.load(array(toId(pokemon.species)), results -> {
                        if (!mAttachedToActivity) return;
                        DexPokemon dexPokemon = results[0];
                        if (dexPokemon == null) { // This pokemon does not have an entry in our dex.json
                            Toast.makeText(getContext(), "The Pokemon you imported does not exist in current pokedex.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (mHasPokemonData) clearSelectedSpecies();
                        bindExistingPokemon(pokemon); // Binding our data
                        bindDexPokemon(dexPokemon); // Setting data from dex
                        mHasPokemonData = true;
                        toggleInputViewsEnabled(true);
                    });
                } else {
                    Toast.makeText(getContext(), "No Pokemon found in clipboard.",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "There is nothing that looks like a Pokemon in clipboard.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toggleInputViewsEnabled(false);
        final TeamPokemon pokemon = (TeamPokemon) getArguments().getSerializable(ARG_PKMN);
        if (pokemon != null) {
            mDexPokemonLoader.load(array(toId(pokemon.species)), results -> {
                if (!mAttachedToActivity) return;
                DexPokemon dexPokemon = results[0];
                if (dexPokemon == null) return; // This pokemon does not have an entry in our dex.json
                bindExistingPokemon(pokemon); // Binding our data
                bindDexPokemon(dexPokemon); // Setting data from dex
                mHasPokemonData = true;
                toggleInputViewsEnabled(true);
            });
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAttachedToActivity = false;
    }

    private void trySpecies(final String species) {
        String[] query = {toId(species)};
        mDexPokemonLoader.load(query, results -> {
            if (!mAttachedToActivity) return;
            DexPokemon dexPokemon = results[0];
            if (dexPokemon == null) {
                mSpeciesTextView.setText(mCurrentSpecies != null ? mCurrentSpecies.name : null);
            } else {
                bindDexPokemon(dexPokemon);
                mHasPokemonData = true;
                toggleInputViewsEnabled(true);
            }
        });
    }

    private void bindDexPokemon(DexPokemon dexPokemon) {
        mCurrentSpecies = new Species();
        mCurrentSpecies.id = toId(dexPokemon.species);
        mCurrentSpecies.name = dexPokemon.species;
        updatePokemonSprite();
        mSpeciesTextView.setText(dexPokemon.species);

        ImageView placeHolderTop = requireView().findViewById(R.id.type1);
        placeHolderTop.setImageResource(Type.getResId(dexPokemon.firstType));
        ImageView placeHolderBottom = requireView().findViewById(R.id.type2);
        if (dexPokemon.secondType != null) placeHolderBottom.setImageResource(Type.getResId(dexPokemon.secondType));
        else placeHolderBottom.setImageDrawable(null);

        List<String> abilities = new LinkedList<>(dexPokemon.abilities);
        if (dexPokemon.hiddenAbility != null)
            abilities.add(dexPokemon.hiddenAbility + " (Hidden)");
        mAbilitySpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                abilities));
        if (mCurrentAbility == null) {
            mCurrentAbility = abilities.get(0);
        } else {
            for (int i = 0; i < abilities.size(); i++) {
                String ability = abilities.get(i);
                if (toId(ability).contains(toId(mCurrentAbility)))
                    mAbilitySpinner.setSelection(i);
            }
        }

        String[] query = {mCurrentSpecies.id};
        mLearnsetLoader.load(query, results -> {
            if (!mAttachedToActivity) return;
            final Set<String> moves = results[0];
            if (moves == null) return;
            for (int i = 0; i < 4; i++) {
                final AutoCompleteTextView textView = mMoveTextViews[i];
                MovesAdapter adapter = new MovesAdapter(mMoveDetailsLoader, moves, mTextHighlightColor);
                textView.setAdapter(adapter);
            }
        });

        Stats stats = dexPokemon.baseStats;
        mCurrentBaseStats = stats;
        mStatsTable.setBaseStats(stats);
    }

    private void bindExistingPokemon(TeamPokemon pokemon) {
            if (!pokemon.species.equalsIgnoreCase(pokemon.name))
                mNameTextView.setText(pokemon.name);
            mLevelTextView.setText(str(pokemon.level));
            mShinyCheckbox.setChecked(pokemon.shiny);
            mHappinessTextView.setText(str(pokemon.happiness));
            mCurrentAbility = pokemon.ability;
            FilterableAdapter<Item> itemAdapter = (FilterableAdapter<Item>) mItemTextView.getAdapter();
            if (itemAdapter != null) {
                for (int i = 0; i < itemAdapter.getCount(); i++) {
                    Item item = itemAdapter.getItem(i);
                    if (item == null || pokemon.item == null) continue;
                    if (pokemon.item.equals(item.id)) {
                        mCurrentItem = item;
                        mItemTextView.setText(item.name);
                    }
                }
            } else {
                mCurrentItem = new Item();
                mCurrentItem.id = mCurrentItem.name = pokemon.item;
            }
            for (int i = 0; i < 4; i++)
                if (i < pokemon.moves.length) mCurrentMoves[i] = pokemon.moves[i];
            if (mCurrentMoves.length > 0) {
                // Retrieve full name for moves
                mMoveDetailsLoader.load(mCurrentMoves, results -> {
                    if (!mAttachedToActivity) return;
                    for (int i = 0; i < results.length; i++) {
                        if (results[i] != null)
                            mMoveTextViews[i].setText(results[i].name);
                    }
                });
            }
            mCurrentEvs = pokemon.evs;
            mStatsTable.setEVs(mCurrentEvs);
            mCurrentIvs = pokemon.ivs;
            mStatsTable.setIVs(mCurrentIvs);
            if (pokemon.nature != null) {
                int index = 0;
                for (int i = 0; i < Nature.ALL.length; i++)
                    if (Nature.ALL[i].name.equalsIgnoreCase(pokemon.nature)) index = i;
                mCurrentNature = Nature.ALL[index];
                mNatureSpinner.setSelection(index);
            }
    }

    private void clearSelectedSpecies() {
        toggleInputViewsEnabled(false);
        mHasPokemonData = false;

        mCurrentSpecies = null;
        updatePokemonSprite();
        mSpeciesTextView.getText().clear();

        ImageView placeHolderTop = requireView().findViewById(R.id.type1);
        placeHolderTop.setImageDrawable(null);
        ImageView placeHolderBottom = requireView().findViewById(R.id.type2);
        placeHolderBottom.setImageDrawable(null);
        mNameTextView.getText().clear();
        mLevelTextView.setText("100");
        mShinyCheckbox.setChecked(false);
        mHappinessTextView.setText("255");
        mCurrentAbility = null;
        mAbilitySpinner.setAdapter(null);
        mCurrentItem = null;
        mItemTextView.getText().clear();
        mCurrentMoves = new String[4];
        for (int i = 0; i < 4; i++)
            mMoveTextViews[i].setAdapter(null);
        mCurrentEvs = new Stats(0);
        mCurrentIvs = new Stats(31);
        mCurrentBaseStats = null;
        mStatsTable.clear();
        mCurrentNature = Nature.DEFAULT;
        mNatureSpinner.setSelection(0);
        ScrollView scrollView = requireView().findViewById(R.id.scrollView);
        scrollView.smoothScrollTo(0, 0);
        mSpeciesTextView.requestFocus();

        updatePokemonNoData();
    }

    private void updatePokemonSprite() {
        if (mCurrentSpecies == null)
            mSpriteImageView.setImageResource(R.drawable.placeholder_pokeball);
        else
            mGlideHelper.loadDexSprite(new BasePokemon(mCurrentSpecies.name), mShinyCheckbox.isChecked(), mSpriteImageView);
    }

    private void toggleInputViewsEnabled(boolean enabled) {
        mClearButton.setEnabled(enabled);
        mClearButton.getIcon().setAlpha(enabled ? 255 : 125);
        mNameTextView.setEnabled(enabled);
        mLevelTextView.setEnabled(enabled);
        mHappinessTextView.setEnabled(enabled);
        //mGenderTextView.setEnabled(enabled);
        mShinyCheckbox.setEnabled(enabled);
        mAbilitySpinner.setEnabled(enabled);
        mItemTextView.setEnabled(enabled);
        for (int i = 0; i < 4; i++)
            mMoveTextViews[i].setEnabled(enabled);
        mStatsTable.setEnabled(enabled);
        mNatureSpinner.setEnabled(enabled);
        mHpTypeSpinner.setEnabled(enabled);
        mExportButton.setEnabled(enabled);
    }

    public void onStatModified(String stat, int ev, int iv) {
        mCurrentEvs.set(Stats.toIndex(stat), ev);
        mCurrentIvs.set(Stats.toIndex(stat), iv);
        mStatsTable.setEVs(mCurrentEvs);
        mStatsTable.setIVs(mCurrentIvs);
        notifyPokemonDataChanged();
    }

    private void notifyPokemonDataChanged() {
        if (!mHasPokemonData || !mAttachedToActivity) return;
        EditTeamActivity activity = (EditTeamActivity) requireActivity();
        activity.onPokemonUpdated(mSlotIndex, buildPokemon());
    }

    private TeamPokemon buildPokemon() {
        TeamPokemon pokemon = new TeamPokemon(mCurrentSpecies.name);
        pokemon.name = mNameTextView.length() > 0 ? mNameTextView.getText()
                .toString() : null;
        pokemon.level = getCurrentLevel();
        pokemon.happiness = getCurrentHappiness();
        pokemon.shiny = mShinyCheckbox.isChecked();
        pokemon.ability = toId(mCurrentAbility);
        pokemon.item = mCurrentItem != null ? mCurrentItem.id : "";
        pokemon.moves = getCurrentMoves();
        pokemon.ivs = mCurrentIvs;
        pokemon.evs = mCurrentEvs;
        pokemon.nature = mCurrentNature.name;
        return pokemon;
    }

    private void updatePokemonNoData() {
        if (!mAttachedToActivity) return;
        EditTeamActivity activity = (EditTeamActivity) requireActivity();
        activity.onPokemonUpdated(mSlotIndex, null);
    }

    private int getCurrentLevel() {
        Integer level = parseInt(mLevelTextView.getText().toString());
        if (level == null) level = 100;
        return level;
    }

    private int getCurrentHappiness() {
        Integer happiness = parseInt(mHappinessTextView.getText().toString());
        if (happiness == null) happiness = 255;
        return happiness;
    }

    private String[] getCurrentMoves() {
        List<String> moves = new LinkedList<>();
        for (String move : mCurrentMoves) {
            if (move != null && move.length() > 0)
                moves.add(toId(move));
        }
        return moves.toArray(new String[0]);
    }

    private static class SpeciesAdapter extends FilterableAdapter<Species> {

        private LayoutInflater mInflater;
        private final DexIconLoader mIconLoader;
        private final int mIconWidth, mIconHeight;

        public SpeciesAdapter(DexIconLoader iconLoader, @NonNull List<Species> objects, int highlightColor) {
            super(objects, highlightColor);
            mIconLoader = iconLoader;
            int size = 32;
            mIconWidth = dpToPx(size);
            mIconHeight = dpToPx(size * 3f/4f);
        }

        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                if (mInflater == null) mInflater = LayoutInflater.from(parent.getContext());
                convertView = mInflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            }

            Species species = getItem(position);
            TextView textView = (TextView) convertView;
            textView.setText(species.name);
            highlightMatch(textView);
            textView.setCompoundDrawables(null, null, null, null); // Remove eventual previous icon
            mIconLoader.load(array(species.id), (results) -> {
                if (results[0] != null && species.name.contentEquals(textView.getText())) {
                    Drawable drawable = new BitmapDrawable(textView.getResources(), results[0]);
                    drawable.setBounds(0, 0, mIconWidth, mIconHeight);
                    textView.setCompoundDrawables(drawable, null, null, null);
                }
            });

            return convertView;
        }

        @Override
        protected boolean matchConstraint(String constraint, Species candidate) {
            return candidate.name.toLowerCase().contains(constraint.toLowerCase());
        }
    }

    private static class MovesAdapter extends FilterableAdapter<String> {

        private final MoveDetailsLoader mLoader;
        private LayoutInflater mInflater;

        MovesAdapter(MoveDetailsLoader loader, Collection<String> moveIds, int highlightColor) {
            super(moveIds, highlightColor);
            mLoader = loader;
        }

        private static class ViewHolder {
            String moveId;
            private final TextView mNameView;
            private final TextView mDetailsView;
            private final ImageView mTypeView;
            private final ImageView mCategoryView;

            ViewHolder(View parent) {
                mNameView = parent.findViewById(R.id.name_view);
                mDetailsView = parent.findViewById(R.id.details_view);
                mTypeView = parent.findViewById(R.id.type_view);
                mCategoryView = parent.findViewById(R.id.category_view);
            }
        }

        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                if (mInflater == null) mInflater = LayoutInflater.from(parent.getContext());
                convertView = mInflater.inflate(R.layout.list_item_move, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final String moveId = getItem(position);
            holder.moveId = moveId;
            holder.mNameView.setText(moveId, TextView.BufferType.SPANNABLE);
            highlightMatch(holder.mNameView);
            holder.mDetailsView.setText(buildDetailsText(-1, -1, -1));
            holder.mTypeView.setImageDrawable(null);
            holder.mTypeView.animate().cancel();
            holder.mTypeView.setAlpha(0f);
            holder.mCategoryView.setImageDrawable(null);
            holder.mCategoryView.animate().cancel();
            holder.mCategoryView.setAlpha(0f);

            mLoader.load(array(moveId), results -> {
                // Check if callback arrives in time
                if (!holder.moveId.equals(moveId)) return;
                Move.Details info = results[0];
                holder.mNameView.setText(info.name, TextView.BufferType.SPANNABLE);
                highlightMatch(holder.mNameView);
                holder.mDetailsView.setText(buildDetailsText(info.pp, info.basePower, info.accuracy));
                holder.mTypeView.setImageResource(Type.getResId(info.type));
                holder.mTypeView.animate().alpha(1f).start();
                holder.mCategoryView.setImageDrawable(new CategoryDrawable(info.category));
                holder.mCategoryView.animate().alpha(1f).start();
            });

            return convertView;
        }

        @Override
        protected void highlightMatch(TextView textView) {
            String constraint = getCurrentConstraint();
            if (constraint == null) return;
            String text = textView.getText().toString().toLowerCase();
            int spaceIndex = text.indexOf(' ');
            text = text.replace(" ", "");
            if (!text.contains(constraint)) return;
            int startIndex = text.indexOf(constraint);
            if (spaceIndex > 0 && startIndex >= spaceIndex) startIndex++;
            int endIndex = startIndex + constraint.length();
            if (spaceIndex > 0 && startIndex < spaceIndex && endIndex > spaceIndex) endIndex++;
            Spannable spannable = (Spannable) textView.getText();
            spannable.setSpan(new BackgroundColorSpan(getHighlightColor()), startIndex, endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private String buildDetailsText(int pp, int bp, int acc) {
            return "PP: " + (pp >= 0 ? pp : "–") + ", BP: " + (bp > 0 ? bp : "–")
                    + ", AC: " + (acc > 0 ? acc : "–");
        }

        @Override
        protected String prepareConstraint(CharSequence constraint) {
            return constraint.toString().toLowerCase().replace(" ", "");
        }

        @Override
        protected boolean matchConstraint(String constraint, String candidate) {
            return candidate.contains(constraint);
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    public static class EditStatDialog extends DialogFragment implements SeekBar.OnSeekBarChangeListener {

        private static final String ARG_STAT_NAME = "arg-stat-name";
        private static final String ARG_STAT_BASE = "arg-stat-base";
        private static final String ARG_STAT_EV = "arg-stat-ev";
        private static final String ARG_STAT_IV = "arg-stat-iv";
        private static final String ARG_LEVEL = "arg-level";
        private static final String ARG_NATURE = "arg-nature";
        private static final String ARG_EVSUM = "arg-evsum";

        public static EditStatDialog newInstance(String name, int base, int ev,
                                 int iv, int level, float nature, int evsum) {
            Bundle args = new Bundle();
            args.putString(ARG_STAT_NAME, name);
            args.putInt(ARG_STAT_BASE, base);
            args.putInt(ARG_STAT_EV, ev);
            args.putInt(ARG_STAT_IV, iv);
            args.putInt(ARG_LEVEL, level);
            args.putFloat(ARG_NATURE, nature);
            EditStatDialog fragment = new EditStatDialog();
            fragment.setArguments(args);
            return fragment;
        }

        private String mStatName;
        private int mLevel;
        private int mBase;
        private int mEv;
        private int mIv;
        private float mNatureModifier;
        private int mEvSum;

        private EditText mEVsValueView;
        private TextView mIVsValueView;

        @SuppressWarnings("ConstantConditions")
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            mStatName = args.getString(ARG_STAT_NAME);
            mLevel = args.getInt(ARG_LEVEL);
            mBase = args.getInt(ARG_STAT_BASE);
            mEv = args.getInt(ARG_STAT_EV);
            mIv = args.getInt(ARG_STAT_IV);
            mEvSum = args.getInt(ARG_EVSUM);
            mNatureModifier = args.getFloat(ARG_NATURE);
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.dialog_edit_stat, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            mEVsValueView = view.findViewById(R.id.evs_text_view);
            mIVsValueView = view.findViewById(R.id.ivs_text_view);
            TextView titleView = view.findViewById(R.id.title_text_view);
            titleView.setText(mStatName);
            final SeekBar seekBar = view.findViewById(R.id.seek_bar_evs);
            seekBar.setOnSeekBarChangeListener(this);
            seekBar.setProgress(mEv);
            mEVsValueView.setText(Integer.toString(mEv));
            SeekBar seekBar2 = view.findViewById(R.id.seek_bar_ivs);
            seekBar2.setOnSeekBarChangeListener(this);
            seekBar2.setProgress(mIv);
            mIVsValueView.setText(Integer.toString(mIv));

            mEVsValueView.addTextChangedListener(new RangeNumberTextWatcher(0, 252));
            mEVsValueView.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                    Integer val = parseInt(editable.toString());
                    if (val != null) seekBar.setProgress(val);
                }
            });

            view.findViewById(R.id.ok_button).setOnClickListener(view1 -> {
                EditPokemonFragment fragment = (EditPokemonFragment) getTargetFragment();
                //noinspection ConstantConditions
                fragment.onStatModified(mStatName, mEv, mIv);
                dismiss();
            });
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
            if (seekBar.getId() == R.id.seek_bar_evs) {
                Integer currentVal = parseInt(mEVsValueView.getText().toString());
                if (currentVal == null || currentVal != progress)
                    mEVsValueView.setText(Integer.toString(progress));
                mEv = progress;
            } else {
                mIVsValueView.setText(Integer.toString(progress));
                mIv = progress;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }


    }
}