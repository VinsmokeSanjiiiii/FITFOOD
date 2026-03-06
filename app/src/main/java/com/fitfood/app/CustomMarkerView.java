package com.fitfood.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import java.util.List;

@SuppressLint("ViewConstructor")
public class CustomMarkerView extends MarkerView {
    private final TextView tvContent;
    private final List<String> dateLabels;

    public CustomMarkerView(Context context, int layoutResource, List<String> dateLabels) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
        this.dateLabels = dateLabels;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        String date = (index >= 0 && index < dateLabels.size()) ? dateLabels.get(index) : "?";
        String valText = String.format(java.util.Locale.getDefault(), "Date: %s\nVal: %.1f", date, e.getY());
        tvContent.setText(valText);
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}