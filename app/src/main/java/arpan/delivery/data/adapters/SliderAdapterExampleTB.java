package arpan.delivery.data.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smarteist.autoimageslider.SliderViewAdapter;

import java.util.ArrayList;
import java.util.List;

import arpan.delivery.R;
import arpan.delivery.data.models.OfferImage;
import arpan.delivery.data.models.SlidingTextItem;
import arpan.delivery.utils.Constants;

public class SliderAdapterExampleTB extends
        SliderViewAdapter<SliderAdapterExampleTB.SliderAdapterVH> {

    private Context context;
    private List<SlidingTextItem> mSliderItems = new ArrayList<>();

    public SliderAdapterExampleTB(Context context) {
        this.context = context;
    }

    public void renewItems(List<SlidingTextItem> sliderItems) {
        this.mSliderItems = sliderItems;
        notifyDataSetChanged();
    }

    public void deleteItem(int position) {
        this.mSliderItems.remove(position);
        notifyDataSetChanged();
    }

    public void addItem(SlidingTextItem sliderItem) {
        this.mSliderItems.add(sliderItem);
        notifyDataSetChanged();
    }

    @Override
    public SliderAdapterVH onCreateViewHolder(ViewGroup parent) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.tb_item_view, null);
        return new SliderAdapterVH(inflate);
    }

    @Override
    public void onBindViewHolder(SliderAdapterVH viewHolder, final int position) {
        SlidingTextItem sliderItem = mSliderItems.get(position);
        viewHolder.specialOfferTextView.setText(sliderItem.getTextTitle());
        viewHolder.specialOfferTextView.setTextColor(Color.parseColor(sliderItem.getTextColorHex()));
        viewHolder.specialOfferTextView.setBackgroundColor(Color.parseColor(sliderItem.getBackgroundColorHex()));
    }

    @Override
    public int getCount() {
        //slider view count could be dynamic size
        return mSliderItems.size();
    }

    class SliderAdapterVH extends SliderViewAdapter.ViewHolder {

        View itemView;
        TextView specialOfferTextView;

        public SliderAdapterVH(View itemView) {
            super(itemView);
            specialOfferTextView = itemView.findViewById(R.id.specialOfferTextView);
            this.itemView = itemView;
        }
    }

}