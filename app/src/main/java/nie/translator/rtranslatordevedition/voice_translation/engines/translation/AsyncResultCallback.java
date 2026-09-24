package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

/** Minimal asynchronous boundary used to keep Google Task objects out of core lifecycle logic. */
public interface AsyncResultCallback<T> {
    void onSuccess(T result);
    void onFailure(Exception error);
}
