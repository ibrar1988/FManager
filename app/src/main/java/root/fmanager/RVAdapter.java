package root.fmanager;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.math.BigDecimal;
import java.net.URLConnection;
import java.util.HashSet;

@SuppressWarnings("Duplicates")
class RVAdapter extends RecyclerView.Adapter<RVAdapter.ViewHolder> implements View.OnClickListener {

    private RVFrag recyclerView;

    private int colorCard, colorCardSelected, iconColor;

    File[] mDataset;
    File[] unfilteredDataset;

    File currentOpenDir;

    private Context context;

    HashSet<File> selectedFiles = new HashSet<>();

    // Provide a suitable constructor (depends on the kind of dataset)
    RVAdapter(RVFrag recyclerView, Context context) {
        this.context = context;
        this.recyclerView = recyclerView;
        colorCard = ((FManagerActivity)recyclerView.getActivity()).colorCard;
        colorCardSelected = ((FManagerActivity)recyclerView.getActivity()).colorCardSelected;
        iconColor = ((FManagerActivity)recyclerView.getActivity()).colorIcon;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        CardView mCardView;
        LinearLayout mCardViewLL;
        ImageView iconCardType;
        ImageView iconLockState;
        TextView fileName;
        TextView fileSize;
        TextView filePath;
        ViewHolder(View v) {
            super(v);
            mCardView = (CardView) v.findViewById(R.id.card_view);
            mCardViewLL = (LinearLayout) v.findViewById(R.id.card_view_ll);
            iconCardType = (ImageView) v.findViewById(R.id.card_type);
            iconLockState = (ImageView) v.findViewById(R.id.card_lock_state);
            fileName = (TextView) v.findViewById(R.id.card_file_name);
            fileSize = (TextView) v.findViewById(R.id.card_file_size);
            filePath = (TextView) v.findViewById(R.id.card_file_path);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_view, parent, false);
        // set the view's size, margins, padding and layout parameters
        return new ViewHolder(v);
    }

    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.mCardView.clearAnimation();
    }

    private void setAnimation(View viewToAnimate) {
        Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        viewToAnimate.startAnimation(animation);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        File fileAtPosition = mDataset[position];

        setAnimation(holder.mCardView);

        if (selectedFiles.contains(fileAtPosition)) {
            holder.mCardViewLL.setBackgroundColor(colorCardSelected);
        } else holder.mCardViewLL.setBackgroundColor(colorCard);

        String lastLine;
        if (fileAtPosition.isFile() && fileAtPosition.canRead() &&
                (lastLine = Utils.lastLineOfFile(fileAtPosition)) != null &&
                lastLine.startsWith("iv:")) {
            holder.iconLockState.setVisibility(View.VISIBLE);
            holder.iconLockState.setColorFilter(iconColor);
        } else {
            holder.iconLockState.setVisibility(View.GONE);
        }

        holder.fileName.setText(fileAtPosition.getName());
        holder.filePath.setText(fileAtPosition.getAbsolutePath());

        if (fileAtPosition.isDirectory()) {
            holder.iconCardType.setImageResource(R.drawable.ic_folder);
            holder.fileSize.setVisibility(View.GONE);
        } else {
            holder.iconCardType.setImageResource(R.drawable.ic_file);
            double fileSize;
            String fileSizeInStr;
            int divideTimes;
            //noinspection StatementWithEmptyBody
            for (fileSize = fileAtPosition.length(), divideTimes = 0;
                         fileSize > 1023; fileSize /= 1024, divideTimes++);
            fileSize = new BigDecimal(fileSize).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
            switch (divideTimes) {
                case 1:
                    fileSizeInStr = fileSize + " KB";
                    break;
                case 2:
                    fileSizeInStr = fileSize + " MB";
                    break;
                case 3:
                    fileSizeInStr = fileSize + " GB";
                    break;
                default:
                    fileSizeInStr = fileSize + " B";
            }
            holder.fileSize.setText(fileSizeInStr);
            holder.fileSize.setVisibility(View.VISIBLE);
        }

        holder.iconCardType.setColorFilter(iconColor);

        holder.mCardView.setOnClickListener(this);
        holder.mCardView.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        v.findViewById(R.id.card_view_ll).setBackgroundColor(colorCardSelected);
                        selectedFiles.add(new File(((TextView)v.findViewById(R.id.card_file_path)).getText().toString()));
                        ((FManagerActivity) recyclerView.getActivity()).selectionModeToggle(selectedFiles.size());
                        return true;
                    }
                }
        );
    }

    void openDirectory (File dirToOpen) {
        currentOpenDir = dirToOpen;
        mDataset = Utils.sort(dirToOpen.listFiles(),
                PreferenceManager.getDefaultSharedPreferences(context).getBoolean("hidden", false));
        notifyDataSetChanged();
        ((FManagerActivity)recyclerView.getActivity()).toolbarOpenDirPath.setText(currentOpenDir.getAbsolutePath());
        unfilteredDataset = mDataset;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.card_view:
                File fileClicked = new File(((TextView)v.findViewById(R.id.card_file_path)).getText().toString());
                if (!selectedFiles.isEmpty()) {
                    if (selectedFiles.remove(fileClicked)) {
                        v.findViewById(R.id.card_view_ll).setBackgroundColor(colorCard);
                    } else {
                        selectedFiles.add(fileClicked);
                        v.findViewById(R.id.card_view_ll).setBackgroundColor(colorCardSelected);
                    }
                    ((FManagerActivity) recyclerView.getActivity()).selectionModeToggle(selectedFiles.size());
                    return;
                }

                String fileName;
                if (!fileClicked.canRead()) {
                    String errMessage;
                    if (fileClicked.isDirectory())
                        errMessage = "Directory requires root access!";
                    else errMessage = "File requires root access!";
                    Toast.makeText(context, errMessage, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (fileClicked.isDirectory()) {
                    currentOpenDir = fileClicked;
                    ((FManagerActivity)recyclerView.getActivity()).toolbarOpenDirPath.setText(fileClicked.getAbsolutePath());
                    openDirectory(fileClicked);
                } else if (fileClicked.isFile() && (fileName = fileClicked.getName()).contains(".")) {
                    Intent newIntent = new Intent(Intent.ACTION_DEFAULT);
                    String mimeType = URLConnection.guessContentTypeFromName(fileName);
                    newIntent.setDataAndType(Uri.fromFile(fileClicked), mimeType);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    try {
                        context.startActivity(newIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(context, "No handler for this type of file.", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case R.id.toolbar_rename:
                final AlertDialog.Builder renameDialog = new AlertDialog.Builder(context);
                LinearLayout layout = new LinearLayout(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layout.setLayoutParams(params);
                layout.setGravity(Gravity.CENTER_HORIZONTAL);
                layout.setOrientation(LinearLayout.HORIZONTAL);

                final EditText edRename = new EditText(context);
                edRename.setEms(10);
                edRename.setHint("new name");

                layout.addView(edRename);
                renameDialog.setView(layout);
                renameDialog.setTitle("Rename to");
                renameDialog.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File file = selectedFiles.iterator().next();
                        if (file.renameTo(new File(
                                        file.getParent() + File.separator + edRename.getText().toString()))) {
                            Toast.makeText(context, "File renamed.", Toast.LENGTH_SHORT).show();
                        } else Toast.makeText(context, "Rename failed.", Toast.LENGTH_SHORT).show();
                        selectedFiles = null;
                        selectedFiles = new HashSet<>();
                        openDirectory(currentOpenDir);
                        ((FManagerActivity) recyclerView.getActivity()).selectionModeToggle(selectedFiles.size());
                    }
                });
                renameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                renameDialog.show();
                break;

            case R.id.toolbar_delete:
                AlertDialog.Builder deleteConfirmationDialog = new AlertDialog.Builder(context);
                deleteConfirmationDialog.setTitle("Confirm delete " + selectedFiles.size() + " items?");
                deleteConfirmationDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (File selectedFile : selectedFiles) {
                            if (!Utils.delete(selectedFile))
                                Toast.makeText(context,
                                        "Cannot delete some files/folders. Permission denied.",
                                        Toast.LENGTH_SHORT).show();
                        }
                        selectedFiles = null;
                        selectedFiles = new HashSet<>();
                        openDirectory(currentOpenDir);
                        ((FManagerActivity) recyclerView.getActivity()).selectionModeToggle(selectedFiles.size());
                    }
                });
                deleteConfirmationDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                deleteConfirmationDialog.show();
                break;

            case R.id.fab:
                boolean folderSelected = false;
                for (final File file : selectedFiles) {
                    if (file.isFile()) {
                        if (!file.canRead() || !file.canWrite()) {
                            Toast.makeText(context, "Cannot encrypt " + file.getName() +
                                    ". Permission denied.", Toast.LENGTH_SHORT);
                            break;
                        }
                        String lastLine = Utils.lastLineOfFile(file);
                        if (lastLine != null && !lastLine.startsWith("iv:")) {
                            final AlertDialog.Builder passwordDialog = new AlertDialog.Builder(context);
                            passwordDialog.setTitle("Encryption password:");

                            layout = new LinearLayout(context);
                            params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            layout.setLayoutParams(params);
                            layout.setPadding(6, 6, 6, 6);
                            layout.setGravity(Gravity.CENTER_HORIZONTAL);
                            layout.setOrientation(LinearLayout.HORIZONTAL);

                            final EditText edPassword = new EditText(context);
                            edPassword.setEms(10);
                            edPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                            layout.addView(edPassword);
                            passwordDialog.setView(layout);
                            passwordDialog.setPositiveButton("Encrypt", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Crypto.encrypt(file, edPassword.getText().toString())) {
                                        selectedFiles = null;
                                        selectedFiles = new HashSet<>();
                                        ((FManagerActivity) recyclerView.getActivity()).selectionModeToggle(selectedFiles.size());
                                        notifyDataSetChanged();
                                    }
                                }
                            });
                            passwordDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            passwordDialog.show();
                        }
                        else {
                            final AlertDialog.Builder passwordDialog = new AlertDialog.Builder(context);
                            passwordDialog.setTitle("Decryption password:");

                            layout = new LinearLayout(context);
                            params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            layout.setLayoutParams(params);
                            layout.setPadding(6, 6, 6, 6);
                            layout.setGravity(Gravity.CENTER_HORIZONTAL);
                            layout.setOrientation(LinearLayout.HORIZONTAL);

                            final EditText edPassword = new EditText(context);
                            edPassword.setEms(10);
                            edPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                            layout.addView(edPassword);
                            passwordDialog.setView(layout);
                            passwordDialog.setPositiveButton("Decrypt", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    int result = Crypto.decrypt(file, edPassword.getText().toString());
                                    if (result == 1) {
                                        selectedFiles = null;
                                        selectedFiles = new HashSet<>();
                                        ((FManagerActivity) recyclerView.getActivity()).selectionModeToggle(selectedFiles.size());
                                        notifyDataSetChanged();
                                    } else if (result == -1) {
                                        Toast.makeText(context, "Invalid password. Try again.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            passwordDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            passwordDialog.show();
                        }
                    } else folderSelected = true;
                }
                if (folderSelected)
                    Toast.makeText(context, "Folder encryption is not supported.", Toast.LENGTH_SHORT).show();
                notifyDataSetChanged();
                break;
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mDataset != null)
            return mDataset.length;
        return 0;
    }
}
