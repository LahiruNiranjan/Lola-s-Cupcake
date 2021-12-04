package com.Lcupcake.Cupcake.LolasCupcakeapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;



public class ProductGridAdapter extends BaseAdapter {

    private final Context context;
    private final int deviceWidth;
    private final int layout;

    private final ArrayList<Products> productList;

    ProductGridAdapter(Context context, int layout, ArrayList<Products> productList, int deviceWidth) {

        this.context = context;
        this.layout = layout;
        this.productList = productList;
        this.deviceWidth = deviceWidth;
    }

    @Override
    public int getCount() {
        return productList.size();
    }

    @Override
    public Object getItem(int itemPosition) {
        return productList.get(itemPosition);
    }

    @Override
    public long getItemId(int itemPosition) {
        return itemPosition;
    }

    private class ViewHolder{
        ImageView productImage;
        TextView productId, productTitle;
    }

    @Override
    public View getView(int itemPosition, View view, ViewGroup viewGroup) {
        View row = view;
        ViewHolder viewHolder = new ViewHolder();

        if(row == null){

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            row = inflater.inflate(layout, null);
            viewHolder.productId = row.findViewById(R.id.productIdGrid);
            viewHolder.productTitle = row.findViewById(R.id.productTitleGrid);
            viewHolder.productImage = row.findViewById(R.id.productImageGrid);
            row.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) row.getTag();
        }


        int gridSize = deviceWidth / 2;
        viewHolder.productImage.getLayoutParams().height = gridSize;
        viewHolder.productImage.getLayoutParams().width = gridSize;

        Products product = productList.get(itemPosition);
        viewHolder.productId.setText(String.valueOf(product.getProductId()));
        viewHolder.productTitle.setText(product.getProductTitle());

        byte[] productImageByte = product.getProductImage();
        Bitmap imageBitmap = BitmapFactory.decodeByteArray(productImageByte, 0, productImageByte.length);
        viewHolder.productImage.setImageBitmap(imageBitmap);

        return row;
    }
}
