package com.ctext.objectdetection.definition;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ctext.R;

import org.jetbrains.annotations.NotNull;

/*
 * A custom adapter to show the row item better for the definitions
 */

public class DefinitionListViewAdapter extends ArrayAdapter<DefinitionRowItem> {
    private Context context;

    public DefinitionListViewAdapter(Context context, int resourceId, List<DefinitionRowItem> items) {
        super(context, resourceId, items);
        this.context = context;
    }

    private static class ViewHolder {
        ImageView iconImageView;
        TextView typeTextView;
        TextView definitionTextView;
        TextView exampleTextView;
    }

    @SuppressLint("InflateParams")
    @NotNull
    public View getView(int position, View convertView, @NotNull ViewGroup parent) {
        ViewHolder holder;
        DefinitionRowItem definitionRowItem = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            assert mInflater != null;
            convertView = mInflater.inflate(R.layout.definition_list_item, null);
            holder = new ViewHolder();
            holder.iconImageView = convertView.findViewById(R.id.iconImageView);
            holder.typeTextView = convertView.findViewById(R.id.typeTextView);
            holder.definitionTextView = convertView.findViewById(R.id.definitionTextView);
            holder.exampleTextView = convertView.findViewById(R.id.exampleTextView);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        assert definitionRowItem != null;
        holder.iconImageView.setImageResource(definitionRowItem.getIcon());

        // If we have any HTML code, we handle it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            holder.typeTextView.setText(Html.fromHtml(definitionRowItem.getType(), Html.FROM_HTML_MODE_COMPACT));
            holder.definitionTextView.setText(Html.fromHtml(definitionRowItem.getDefinition(), Html.FROM_HTML_MODE_COMPACT));
            holder.exampleTextView.setText(Html.fromHtml(definitionRowItem.getExample(), Html.FROM_HTML_MODE_COMPACT));
        } else {
            holder.typeTextView.setText(Html.fromHtml(definitionRowItem.getType()));
            holder.definitionTextView.setText(Html.fromHtml(definitionRowItem.getDefinition()));
            holder.exampleTextView.setText(Html.fromHtml(definitionRowItem.getExample()));
        }

        return convertView;
    }
}
