package root.fmanager;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import java.io.File;
import java.util.TreeSet;

@SuppressWarnings("WeakerAccess")
public class RVFrag extends Fragment implements Filterable {

    private RVAdapter mAdapter;
    private Context context;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_view, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        final RecyclerView mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        if (mRecyclerView != null) {

            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            mRecyclerView.setHasFixedSize(true);

            // use a linear layout manager
            mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

            // specify an adapter (see also next example)
            mAdapter = new RVAdapter(this, context);
            mRecyclerView.setAdapter(mAdapter);
            mAdapter.openDirectory(new File(getArguments().getString("homeDirPath", "/")));
        }
    }

    public RVAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public Filter getFilter() {
        return new RVFilter();
    }

    @SuppressWarnings("unchecked")
    private class RVFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint.length() == 0) {
                results.values = new TreeSet<>(mAdapter.unfilteredDataset);
                return results;
            } else {
                results.values = new TreeSet<File>();
                for (File file : mAdapter.unfilteredDataset) {
                    if (file.getName().toLowerCase().contains(constraint)) {
                        ((TreeSet<File>)results.values).add(file);
                    }
                }
            }
            results.count = ((TreeSet<File>)results.values).size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            /*mAdapter.mDataset = Utils.sort(((TreeSet<File>)results.values).
                    toArray(new File[((TreeSet<File>)results.values).size()]),
                    PreferenceManager.getDefaultSharedPreferences(context).getBoolean("hidden", false));*/
            mAdapter.mDataset.clear();
            mAdapter.mDataset.addAll((TreeSet<File>)results.values);
            mAdapter.notifyDataSetChanged();
        }
    }

    void openDirectory(File dirToOpen) {
        mAdapter.openDirectory(dirToOpen);
    }

    File getCurrentOpenDir() {
        return mAdapter.currentOpenDir;
    }
}
