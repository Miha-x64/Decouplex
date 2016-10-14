package net.aquadc.decouplex.example;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import net.aquadc.decouplex.DecouplexBuilder;
import net.aquadc.decouplex.DecouplexFragmentCompat;
import net.aquadc.decouplex.annotation.Debounce;
import net.aquadc.decouplex.annotation.OnResult;

/**
 * Created by miha on 17.08.16
 */
public class DebounceFragment extends DecouplexFragmentCompat implements TextWatcher {

    private TextView outputView;
    private QueryHandler queryHandler;

    @Nullable @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        queryHandler = new DecouplexBuilder<>(QueryHandler.class, queryHandlerImpl, getClass()).create(getActivity());
        return inflater.inflate(R.layout.fragment_debounce, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ((EditText) view.findViewById(R.id.debounce_input)).addTextChangedListener(this);
        outputView = (TextView) view.findViewById(R.id.debounce_output);
    }

    @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
    @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
    @Override
    public void afterTextChanged(Editable editable) {
        queryHandler.handle(editable.toString());   // send new text to the handler
    }

    // sample handler — returns unmodified string
    private static final QueryHandler queryHandlerImpl = input -> input;

    @OnResult("handle") // show handled text
    void onHandleResult(String output) {            // get processed result, returned by handler
        outputView.setText(output);
    }

    @FunctionalInterface
    interface QueryHandler {
        @Debounce(300)
        String handle(String input);
    }
}
