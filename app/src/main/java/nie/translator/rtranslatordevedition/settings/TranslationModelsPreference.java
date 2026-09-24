package nie.translator.rtranslatordevedition.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import nie.translator.rtranslatordevedition.R;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.translation.ModelDownloadPolicy;
import nie.translator.rtranslatordevedition.voice_translation.engines.translation.TranslationModel;
import nie.translator.rtranslatordevedition.voice_translation.engines.translation.TranslationModelManager;
import nie.translator.rtranslatordevedition.voice_translation.engines.translation.TranslationModelState;

/** Settings entry for explicit, lifecycle-safe management of ML Kit translation models. */
public class TranslationModelsPreference extends Preference {
    private static final String ANY_NETWORK_KEY = "translation_models_any_network";
    private TranslationModelManager manager;
    private AlertDialog dialog;
    private AlertDialog networkConfirmation;
    private ModelAdapter adapter;
    private TextView errorView;
    private ProgressBar loading;
    private boolean closed;
    private boolean adjustingNetworkPolicy;

    public TranslationModelsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    public TranslationModelsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public TranslationModelsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public TranslationModelsPreference(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference preference) {
                showModels();
                return true;
            }
        });
    }

    public void close() {
        if (closed) { return; }
        closed = true;
        if (manager != null) {
            manager.clearObserver();
            manager.close();
            manager = null;
        }
        if (dialog != null) {
            dialog.setOnDismissListener(null);
            dialog.dismiss();
            dialog = null;
        }
        dismissNetworkConfirmation();
        adapter = null;
        errorView = null;
        loading = null;
    }

    private void showModels() {
        if (closed) { return; }
        if (dialog != null && dialog.isShowing()) {
            return;
        }
        final View content = LayoutInflater.from(getContext()).inflate(R.layout.dialog_translation_models, null);
        adapter = new ModelAdapter(getContext());
        ListView list = content.findViewById(R.id.translation_models_list);
        list.setAdapter(adapter);
        errorView = content.findViewById(R.id.translation_models_error);
        loading = content.findViewById(R.id.translation_models_loading);
        configureNetworkPolicy(content);

        dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.translation_models_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnDismissListener(ignored -> detachDialog());
        dialog.show();

        manager = new TranslationModelManager();
        manager.setObserver(new TranslationModelManager.Observer() {
            @Override public void onModelChanged(TranslationModel model) {
                if (!isUiActive()) { return; }
                adapter.replace(model);
                adapter.notifyDataSetChanged();
            }
        });
        loading.setVisibility(View.VISIBLE);
        manager.refresh(new TranslationModelManager.ModelsCallback() {
            @Override public void onModels(List<TranslationModel> models) {
                if (!isUiActive()) { return; }
                loading.setVisibility(View.GONE);
                errorView.setVisibility(View.GONE);
                adapter.setModels(models);
            }

            @Override public void onFailure(EngineError error) {
                if (!isUiActive()) { return; }
                loading.setVisibility(View.GONE);
                showError(error);
            }
        });
    }

    private void configureNetworkPolicy(View content) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final RadioGroup group = content.findViewById(R.id.translation_models_network_policy);
        group.check(preferences.getBoolean(ANY_NETWORK_KEY, false)
                ? R.id.translation_models_any_network : R.id.translation_models_wifi_only);
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                if (adjustingNetworkPolicy) { return; }
                if (checkedId == R.id.translation_models_wifi_only) {
                    preferences.edit().putBoolean(ANY_NETWORK_KEY, false).apply();
                } else if (checkedId == R.id.translation_models_any_network) {
                    if (preferences.getBoolean(ANY_NETWORK_KEY, false)) { return; }
                    confirmAnyNetwork(preferences, radioGroup);
                }
            }
        });
    }

    private void confirmAnyNetwork(final SharedPreferences preferences, final RadioGroup group) {
        dismissNetworkConfirmation();
        final boolean[] confirmed = new boolean[] { false };
        final AlertDialog confirmation = new AlertDialog.Builder(getContext())
                .setTitle(R.string.translation_models_any_network_confirmation_title)
                .setMessage(R.string.translation_models_any_network_confirmation_message)
                .setPositiveButton(R.string.translation_models_any_network_confirmation_continue,
                        (ignored, which) -> {
                            confirmed[0] = true;
                            preferences.edit().putBoolean(ANY_NETWORK_KEY, true).apply();
                        })
                .setNegativeButton(android.R.string.cancel, (ignored, which) ->
                        revertToWifiOnly(preferences, group))
                .create();
        confirmation.setOnDismissListener(ignored -> {
            if (!confirmed[0]) {
                revertToWifiOnly(preferences, group);
            }
            if (networkConfirmation == confirmation) {
                networkConfirmation = null;
            }
        });
        networkConfirmation = confirmation;
        confirmation.show();
    }

    private void revertToWifiOnly(SharedPreferences preferences, RadioGroup group) {
        preferences.edit().putBoolean(ANY_NETWORK_KEY, false).apply();
        if (group.getCheckedRadioButtonId() == R.id.translation_models_wifi_only) { return; }
        adjustingNetworkPolicy = true;
        group.check(R.id.translation_models_wifi_only);
        adjustingNetworkPolicy = false;
    }

    private void dismissNetworkConfirmation() {
        if (networkConfirmation != null) {
            networkConfirmation.setOnDismissListener(null);
            networkConfirmation.dismiss();
            networkConfirmation = null;
        }
    }

    private ModelDownloadPolicy selectedPolicy() {
        boolean anyNetwork = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean(ANY_NETWORK_KEY, false);
        return anyNetwork ? ModelDownloadPolicy.ANY_NETWORK : ModelDownloadPolicy.WIFI_ONLY;
    }

    private void runAction(final TranslationModel model) {
        if (!isUiActive()) { return; }
        errorView.setVisibility(View.GONE);
        TranslationModelManager.OperationCallback callback = new TranslationModelManager.OperationCallback() {
            @Override public void onSuccess(TranslationModel updated) {
                if (isUiActive()) { errorView.setVisibility(View.GONE); }
            }

            @Override public void onFailure(EngineError error) {
                if (isUiActive()) { showError(error); }
            }
        };
        if (model.getState() == TranslationModelState.DOWNLOADED) {
            manager.delete(model.getLanguageTag(), callback);
        } else {
            manager.download(model.getLanguageTag(), selectedPolicy(), callback);
        }
    }

    private void showError(EngineError error) {
        errorView.setText(errorText(error));
        errorView.setVisibility(View.VISIBLE);
    }

    private int errorText(EngineError error) {
        switch (error.getCategory()) {
            case NETWORK:
                return R.string.translation_models_error_network;
            case BUSY:
                return R.string.translation_models_error_busy;
            case QUOTA:
                return R.string.translation_models_error_storage;
            case UNSUPPORTED_LANGUAGE:
            case UNSUPPORTED:
                return R.string.translation_models_error_unsupported;
            default:
                return R.string.translation_models_error_generic;
        }
    }

    private boolean isUiActive() {
        return !closed && dialog != null && dialog.isShowing() && manager != null;
    }

    private void detachDialog() {
        dismissNetworkConfirmation();
        if (manager != null) {
            manager.clearObserver();
            manager.close();
            manager = null;
        }
        dialog = null;
        adapter = null;
        errorView = null;
        loading = null;
    }

    private final class ModelAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private final List<TranslationModel> models = new ArrayList<>();

        private ModelAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        private void setModels(List<TranslationModel> updated) {
            models.clear();
            models.addAll(updated);
            notifyDataSetChanged();
        }

        private void replace(TranslationModel updated) {
            for (int index = 0; index < models.size(); index++) {
                if (models.get(index).getLanguageTag().equals(updated.getLanguageTag())) {
                    models.set(index, updated);
                    return;
                }
            }
            models.add(updated);
        }

        @Override public int getCount() { return models.size(); }
        @Override public TranslationModel getItem(int position) { return models.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = inflater.inflate(R.layout.component_row_translation_model, parent, false);
            }
            final TranslationModel model = getItem(position);
            TextView language = row.findViewById(R.id.translation_model_language);
            TextView state = row.findViewById(R.id.translation_model_state);
            ProgressBar progress = row.findViewById(R.id.translation_model_progress);
            Button action = row.findViewById(R.id.translation_model_action);
            Locale locale = Locale.forLanguageTag(model.getLanguageTag());
            language.setText(locale.getDisplayName(Locale.getDefault()));
            state.setText(stateText(model.getState()));
            boolean active = model.getState() == TranslationModelState.QUEUED
                    || model.getState() == TranslationModelState.DOWNLOADING
                    || model.getState() == TranslationModelState.DELETING;
            progress.setVisibility(active ? View.VISIBLE : View.GONE);
            action.setVisibility(active || model.getState() == TranslationModelState.BUILT_IN
                    ? View.GONE : View.VISIBLE);
            action.setText(model.getState() == TranslationModelState.DOWNLOADED
                    ? R.string.translation_models_delete : R.string.translation_models_download);
            action.setEnabled(!TranslateLanguage.ENGLISH.equals(model.getLanguageTag()));
            action.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { runAction(model); }
            });
            return row;
        }
    }

    private int stateText(TranslationModelState state) {
        switch (state) {
            case BUILT_IN: return R.string.translation_models_state_built_in;
            case NOT_DOWNLOADED: return R.string.translation_models_state_not_downloaded;
            case QUEUED: return R.string.translation_models_state_queued;
            case DOWNLOADING: return R.string.translation_models_state_downloading;
            case DOWNLOADED: return R.string.translation_models_state_downloaded;
            case DELETING: return R.string.translation_models_state_deleting;
            case FAILED: return R.string.translation_models_state_failed;
            default: return R.string.translation_models_error_generic;
        }
    }
}
