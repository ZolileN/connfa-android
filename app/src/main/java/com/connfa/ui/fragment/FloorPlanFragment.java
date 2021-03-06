package com.connfa.ui.fragment;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.connfa.R;
import com.connfa.model.Model;
import com.connfa.model.UpdatesManager;
import com.connfa.model.data.FloorPlan;
import com.connfa.ui.adapter.FloorSelectorAdapter;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.util.ArrayList;
import java.util.List;

public class FloorPlanFragment extends Fragment {

    private static int RECOMMENDED_FLOOR_IMAGE_HEIGHT = Resources.getSystem().getDisplayMetrics().heightPixels * 2;
    private static int RECOMMENDED_FLOOR_IMAGE_WIDTH = Resources.getSystem().getDisplayMetrics().widthPixels * 2;

    public static final String TAG = "FloorPlanFragment";

    private View mLayoutContent, mLayoutPlaceholder;
    private Spinner floorSelector;
    private List<FloorPlan> plans;
    private SubsamplingScaleImageView floorImage;

    private UpdatesManager.DataUpdatedListener updateListener = new UpdatesManager.DataUpdatedListener() {
        @Override
        public void onDataUpdated(List<Integer> requestIds) {

            if (requestIds.contains(UpdatesManager.FLOOR_PLANS_REQUEST_ID)) {
                new LoadPlansTask().execute();
            }

        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Model.getInstance().getUpdatesManager().registerUpdateListener(updateListener);
    }

    @Override
    public void onDestroy() {
        Model.getInstance().getUpdatesManager().unregisterUpdateListener(updateListener);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View result = inflater.inflate(R.layout.fr_floor_plan, container, false);
        mLayoutContent = result.findViewById(R.id.layout_content);
        mLayoutPlaceholder = result.findViewById(R.id.layout_placeholder);
        floorSelector = (Spinner) result.findViewById(R.id.spinner);
        floorSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                new LoadPlanImageTask().execute(plans.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        floorImage = (SubsamplingScaleImageView) result.findViewById(R.id.floor_plan_image);

        new LoadPlansTask().execute();

        return result;
    }

    private class LoadPlansTask extends AsyncTask<Void, Void, List<FloorPlan>> {

        @Override
        protected List<FloorPlan> doInBackground(Void... params) {
            return Model.getInstance().getFloorPlansManager().getFloorPlans();
        }

        @Override
        protected void onPostExecute(List<FloorPlan> floorPlans) {
            super.onPostExecute(floorPlans);
            plans = floorPlans;

            if (plans == null || plans.isEmpty()) {
                mLayoutContent.setVisibility(View.GONE);
                mLayoutPlaceholder.setVisibility(View.VISIBLE);
            } else {
                mLayoutContent.setVisibility(View.VISIBLE);
                mLayoutPlaceholder.setVisibility(View.GONE);

                List<String> names = new ArrayList<>(floorPlans.size());
                for (FloorPlan plan : plans) {
                    names.add(plan.getName());
                }

                FloorSelectorAdapter floorsAdapter = new FloorSelectorAdapter(floorSelector.getContext(), names);
                floorSelector.setAdapter(floorsAdapter);

                floorSelector.setVisibility(plans.isEmpty() ? View.INVISIBLE : View.VISIBLE);
            }
        }
    }

    private class LoadPlanImageTask extends AsyncTask<FloorPlan, Void, ImageSource> {

        @Override
        protected void onPreExecute() {
            floorSelector.setEnabled(false);
            floorSelector.setClickable(false);
        }

        @Override
        protected ImageSource doInBackground(FloorPlan... params) {
            Bitmap planImage = Model.getInstance()
                    .getFloorPlansManager()
                    .getImageForPlan(
                            params[0],
                            RECOMMENDED_FLOOR_IMAGE_WIDTH,
                            RECOMMENDED_FLOOR_IMAGE_HEIGHT
                    );

            if (planImage != null) {
                return ImageSource.bitmap(planImage);
            }
            return null;
        }

        @Override
        protected void onPostExecute(ImageSource imageSource) {
            floorSelector.setEnabled(true);
            floorSelector.setClickable(true);
            floorImage.setImage(imageSource);
            floorImage.resetScaleAndCenter();
        }
    }

}
