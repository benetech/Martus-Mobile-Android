package org.martus.android.dialog;

import org.martus.android.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;

/**
 * @author roms
 *         Date: 12/23/12
 */
public class InstallExplorerDialog extends DialogFragment {

    public InstallExplorerDialog() {
        // Empty constructor required for DialogFragment
    }

    public static InstallExplorerDialog newInstance() {
        InstallExplorerDialog frag = new InstallExplorerDialog();
        Bundle args = new Bundle();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        return new AlertDialog.Builder(getActivity())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setView(inflater.inflate(R.layout.install_file_explorer, null))
            .setTitle("Try another file explorer")
            .create();
    }
}
