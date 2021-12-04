package com.Lcupcake.Cupcake.LolasCupcakeapp;

import android.app.ActivityOptions;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.os.Bundle;
import android.app.Activity;
import android.provider.BaseColumns;
import android.support.design.widget.FloatingActionButton;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class UserMenu extends Activity implements View.OnClickListener, SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, SearchView.OnCloseListener, AdapterView.OnItemClickListener {

    static final int VIEW_MODE_LISTVIEW = 0;
    static final int VIEW_MODE_GRIDVIEW = 1;
    private static final String[] COLUMNS = {
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_ICON_1
    };
    public String memberId;
    public static boolean isUserLoggedIn = false;
    private static SQLLiteHelperProducts sqLiteHelper;
    public int cartCount = 0;
    public String randomID;
    public Dialog checkoutDialog;
    FloatingActionButton FilterFab, FilterCheapFab, FilterExpensiveFab, SwitchGridView, SwitchListView, ClearCartFab;
    ViewSwitcher FabViewSwitcher;
    TextView CartNetTotal;
    Animation FabOpen, FabClose, FabTranslateUp, FabTranslateDown;
    Button CheckoutButton, ConfirmCheckoutButton;
    SearchView UserSearchView;
    ViewStub GridViewStub;
    ViewStub ListViewStub;
    ListView ProductListView, CheckoutItemsListView;
    GridView ProductGridView;
    ArrayList<CartItems> CartItemsList;
    ArrayList<Products> ProductList;
    ArrayList<Integer> ProductIDList;
    CheckoutListAdapter CheckoutItemsListAdapter;
    ProductListAdapter ListAdapter;
    ProductGridAdapter GridAdapter;
    boolean isOpen = false;
    private SearchSuggestionsAdapter mSuggestionsAdapter;
    private int deviceWidth;
    private int currentViewMode = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_menu);
        getDeviceWidth();
        initializeComponents();
        initializeViewModes();
        initializeListeners();
        checkLoginStatus();
        checkView();
        generateRandomID();
        sqlLiteDB();
        getAllData();
        floatingButtonAnimation();
        getAllSearchSuggestions();
        getCartCounter();
        checkCartCount();
        clearCart();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getCartCounter();
        checkLoginStatus();
        checkCartCount();
        getAllData();
    }

    private void getCartCounter() {
        cartCount = sqLiteHelper.getCartCount(randomID);
    }

    private void checkLoginStatus() {
        if(getIntent().getExtras() == null){
            isUserLoggedIn = false;
        } else {
            isUserLoggedIn = true;
            memberId = getIntent().getExtras().get("userId").toString();
        }
    }


    private void initializeComponents() {
        ListViewStub = findViewById(R.id.userListViewStub);
        GridViewStub = findViewById(R.id.userGridViewStub);
        FabViewSwitcher = findViewById(R.id.fab_view_switcher);

        checkoutDialog = new Dialog(UserMenu.this);
        checkoutDialog.setContentView(R.layout.user_checkout_dialog);
        CartNetTotal = checkoutDialog.findViewById(R.id.txtNetTotal);

        CheckoutItemsListView = checkoutDialog.findViewById(R.id.userCheckoutList);
        CartItemsList = new ArrayList<>();
        CheckoutItemsListAdapter = new CheckoutListAdapter(checkoutDialog.getContext(), R.layout.user_checkout_product_list_view, CartItemsList);
        CheckoutItemsListView.setAdapter(CheckoutItemsListAdapter);

        ConfirmCheckoutButton = checkoutDialog.findViewById(R.id.btnConfirmCheckout);

        FilterFab = findViewById(R.id.filter_fab);
        FilterCheapFab = findViewById(R.id.filter_cheap_fab);
        FilterExpensiveFab = findViewById(R.id.filter_expensive_fab);
        ClearCartFab = findViewById(R.id.clear_cart_fab);
        SwitchGridView = findViewById(R.id.fab_view_grid);
        SwitchListView = findViewById(R.id.fab_view_list);
        CheckoutButton = findViewById(R.id.userCheckoutButton);
        UserSearchView = findViewById(R.id.userSearchView);

        AutoCompleteTextView searchAutoCompleteTextView = UserSearchView.findViewById(UserSearchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null));
        searchAutoCompleteTextView.setThreshold(0);
        ProductList = new ArrayList<>();
        ProductIDList = new ArrayList<>();
        ListAdapter = new ProductListAdapter(UserMenu.this, R.layout.user_product_items_list_view, ProductList);
        GridAdapter = new ProductGridAdapter(UserMenu.this, R.layout.user_product_items_grid_view, ProductList, deviceWidth);
    }

    private void initializeListeners() {
        FilterFab.setOnClickListener(this);
        FilterCheapFab.setOnClickListener(this);
        FilterExpensiveFab.setOnClickListener(this);
        ProductGridView.setOnItemClickListener(this);
        ProductListView.setOnItemClickListener(this);
        UserSearchView.setOnQueryTextListener(this);
        UserSearchView.setOnSuggestionListener(this);
        UserSearchView.setOnCloseListener(this);
        CheckoutButton.setOnClickListener(this);
        ConfirmCheckoutButton.setOnClickListener(this);
        SwitchGridView.setOnClickListener(this);
        SwitchListView.setOnClickListener(this);
        ClearCartFab.setOnClickListener(this);

        ProductListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int currentVisibleItemCount;
            private int currentScrollState;
            private int currentFirstVisibleItem;
            private int totalItem;

            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                this.currentScrollState = scrollState;
                if(currentScrollState == SCROLL_STATE_FLING){
                    UserSearchView.setAlpha(0.0f);
                } else if(currentScrollState == SCROLL_STATE_TOUCH_SCROLL){
                    UserSearchView.setAlpha(0.5f);
                } else if(currentScrollState == SCROLL_STATE_IDLE){
                    UserSearchView.setAlpha(1);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                this.currentFirstVisibleItem = firstVisibleItem;
                this.currentVisibleItemCount = visibleItemCount;
                this.totalItem = totalItemCount;
            }
        });

        CheckoutItemsListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();

                getCheckoutNetTotal();
                getCartCounter();
                checkCartCount();
            }
        });
    }


    private void initializeViewModes() {

        ProductListView = (ListView) ListViewStub.inflate();
        ProductGridView = (GridView) GridViewStub.inflate();

        SharedPreferences sharedPreferences = getSharedPreferences("com.cmbpizza.razor.colombopizza.viewMode", MODE_PRIVATE);

        currentViewMode = sharedPreferences.getInt("com.cmbpizza.razor.colombopizza.currentViewMode", VIEW_MODE_LISTVIEW);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.filter_fab:
                filterFab();
                break;
            case R.id.filter_cheap_fab:
                sortProductList(0);
                break;
            case R.id.filter_expensive_fab:
                sortProductList(1);
                //clearCart();
                //checkCartCount();
                break;
            case R.id.fab_view_grid:
                switchViewMode();
                break;
            case R.id.fab_view_list:
                switchViewMode();
                break;
            case R.id.clear_cart_fab:
                clearCart();
                checkCartCount();
                break;
            case R.id.userCheckoutButton:
                checkoutDialog();
                break;
            case R.id.btnConfirmCheckout:
                createOrder();
                break;
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        if (currentViewMode == VIEW_MODE_GRIDVIEW) {
            TextView ListProductId = view.findViewById(R.id.productIdGrid);
            Intent newIntent = new Intent(UserMenu.this, UserProductView.class);

            Pair<View, String> pairImage = Pair.create(view.findViewById(R.id.productImageGrid), "productImage");
            Pair<View, String> pairTitle = Pair.create(view.findViewById(R.id.productTitleGrid), "productTitle");

            ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(
                    UserMenu.this,
                    pairImage,
                    pairTitle);
            newIntent.putExtra("userId", memberId);
            newIntent.putExtra("productId", ListProductId.getText().toString());
            newIntent.putExtra("cartId", randomID);

            startActivity(newIntent, activityOptions.toBundle());
        } else {
            TextView ListProductId = view.findViewById(R.id.productId);
            Intent newIntent = new Intent(UserMenu.this, UserProductView.class);

            Pair<View, String> pairImage = Pair.create(view.findViewById(R.id.productImage), "productImage");
            Pair<View, String> pairTitle = Pair.create(view.findViewById(R.id.productTitle), "productTitle");
            Pair<View, String> pairDescription = Pair.create(view.findViewById(R.id.productDescription), "productDescription");
            Pair<View, String> pairCategory = Pair.create(view.findViewById(R.id.productCategory), "productCategory");
            Pair<View, String> pairPrice = Pair.create(view.findViewById(R.id.productPrice), "productPrice");

            ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(
                    UserMenu.this,
                    pairImage,
                    pairTitle,
                    pairDescription,
                    pairCategory,
                    pairPrice);
            newIntent.putExtra("userId", memberId);
            newIntent.putExtra("productId", ListProductId.getText().toString());
            newIntent.putExtra("cartId", randomID);

            startActivity(newIntent, activityOptions.toBundle());
        }
    }


    private void checkView() {
        if (currentViewMode == VIEW_MODE_GRIDVIEW) {
            //displaying the grid view and hiding the list view
            GridViewStub.setVisibility(View.VISIBLE);
            ListViewStub.setVisibility(View.GONE);
            ProductGridView.setAdapter(GridAdapter);
            if(FabViewSwitcher.getCurrentView() == SwitchGridView){
                FabViewSwitcher.showNext();
            }
        } else {

            ListViewStub.setVisibility(View.VISIBLE);
            GridViewStub.setVisibility(View.GONE);
            ProductListView.setAdapter(ListAdapter);
            FabViewSwitcher.showNext();
            if(FabViewSwitcher.getCurrentView() == SwitchListView){
                FabViewSwitcher.showNext();
            }
        }
    }

    private void switchViewMode() {
        if (currentViewMode == VIEW_MODE_GRIDVIEW) {

            currentViewMode = VIEW_MODE_LISTVIEW;
        } else {

            currentViewMode = VIEW_MODE_GRIDVIEW;
        }

        checkView();

        SharedPreferences sharedPreferences = getSharedPreferences("com.cmbpizza.razor.colombopizza.viewMode", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("com.cmbpizza.razor.colombopizza.currentViewMode", currentViewMode);
        editor.apply();
    }

    private void getDeviceWidth() {

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        deviceWidth = size.x;
    }


    private void floatingButtonAnimation() {
        FabOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        FabClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        FabTranslateUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_translate_up);
        FabTranslateDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_translate_down);
    }


    private void filterFab() {
        if (isOpen) {
            FilterFab.startAnimation(FabTranslateDown);
            FilterCheapFab.startAnimation(FabClose);
            FilterExpensiveFab.startAnimation(FabClose);
            FilterCheapFab.setClickable(false);
            FilterExpensiveFab.setClickable(false);
            isOpen = false;
        } else {
            FilterFab.startAnimation(FabTranslateUp);
            FilterCheapFab.startAnimation(FabOpen);
            FilterExpensiveFab.startAnimation(FabOpen);
            FilterCheapFab.setClickable(true);
            FilterExpensiveFab.setClickable(true);
            isOpen = true;
        }
    }

    //this method is used to create a database
    private void sqlLiteDB() {
        sqLiteHelper = new SQLLiteHelperProducts(UserMenu.this, "ProductDB", null, 1);
        sqLiteHelper.dataQuery("CREATE TABLE IF NOT EXISTS productTable (productId INTEGER PRIMARY KEY AUTOINCREMENT, productTitle VARCHAR, productCategory VARCHAR, productPrice INTEGER, productDescription VARCHAR, productImage BLOB)");
        sqLiteHelper.dataQuery("CREATE TABLE IF NOT EXISTS productCartTable (id INTEGER PRIMARY KEY AUTOINCREMENT, cartId VARCHAR, productId INTEGER, productQuantity INTEGER)");
        sqLiteHelper.dataQuery("CREATE TABLE IF NOT EXISTS productOrderTable (orderId INTEGER PRIMARY KEY, memberId INTEGER, idList VARCHAR, quantityList VARCHAR, totalPrice INTEGER, orderStatus INTEGER)");
    }

    //this method is used to create a database
    private void clearCart() {
        sqLiteHelper.dataQuery("DELETE FROM productCartTable");
        sqLiteHelper.dataQuery("DELETE FROM sqlite_sequence WHERE name='productCartTable'");
        cartCount = 0;
        Log.d("sql", "cart cleared");
    }

    //get all the data from the sqlLite database
    private void getAllData() {
        ProductIDList.clear();
        ProductList.clear();
        for (Products product : sqLiteHelper.getAllData()) {
            ProductIDList.add(product.getProductId());
            ProductList.add(new Products(product.getProductId(), product.getProductTitle(), product.getProductCategory(), product.getProductPrice(), product.getProductDescription(), product.getProductImage()));
        }
        GridAdapter.notifyDataSetChanged();
        ListAdapter.notifyDataSetChanged();
    }


    private void getAllSearchSuggestions() {
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        for (int i : ProductIDList) {
            cursor.addRow(new String[]{String.valueOf(sqLiteHelper.getProductID(i)), sqLiteHelper.getProductTitle(i), android.util.Base64.encodeToString(sqLiteHelper.getProductImage(i), Base64.DEFAULT)});
        }
        mSuggestionsAdapter = new SearchSuggestionsAdapter(UserSearchView.getContext(), cursor, R.layout.search_suggestion_view_user, true);
        UserSearchView.setSuggestionsAdapter(mSuggestionsAdapter);
    }

    public void getSearchTextSuggestions(String searchString) {
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        ArrayList<Products> filterProductList = new ArrayList<>();
        filterProductList.clear();
        if (!searchString.isEmpty()) {
            String searchStringLower = searchString.toLowerCase();
            for (Products list : ProductList) {
                String ProductName = list.getProductTitle().toLowerCase();
                if (ProductName.startsWith(searchStringLower)) {
                    filterProductList.add(list);
                }
            }
            for (Products filterList : filterProductList) {
                cursor.addRow(new String[]{String.valueOf(filterList.getProductId()), filterList.getProductTitle(), android.util.Base64.encodeToString(filterList.getProductImage(), Base64.DEFAULT)});
            }
        } else {
            for (int i : ProductIDList) {
                cursor.addRow(new String[]{String.valueOf(sqLiteHelper.getProductID(i)), sqLiteHelper.getProductTitle(i), android.util.Base64.encodeToString(sqLiteHelper.getProductImage(i), Base64.DEFAULT)});
            }
        }
        mSuggestionsAdapter = new SearchSuggestionsAdapter(UserSearchView.getContext(), cursor, R.layout.search_suggestion_view, false);
        UserSearchView.setSuggestionsAdapter(mSuggestionsAdapter);
    }


    @Override
    public boolean onClose() {
        getAllData();
        return true;
    }


    @Override
    public boolean onQueryTextSubmit(String s) {
        String searchStringLower = s.toLowerCase();
        ArrayList<Products> filterProductList = new ArrayList<>();
        for (Products list : ProductList) {
            String ProductName = list.getProductTitle().toLowerCase();
            if (ProductName.startsWith(searchStringLower)) {
                filterProductList.add(list);
            }
        }
        ProductList.clear();
        ProductList.addAll(filterProductList);
        ListAdapter.notifyDataSetChanged();
        GridAdapter.notifyDataSetChanged();
        filterProductList.clear();
        Toast.makeText(this, "You searched for: " + s, Toast.LENGTH_LONG).show();
        return true;
    }


    @Override
    public boolean onQueryTextChange(String searchString) {
        if (!searchString.isEmpty()) {
            getSearchTextSuggestions(searchString);
        } else {
            getAllSearchSuggestions();
            getAllData();
        }
        return true;
    }

    @Override
    public boolean onSuggestionSelect(int i) {
        return false;
    }


    @Override
    public boolean onSuggestionClick(int i) {
        Cursor c = (Cursor) mSuggestionsAdapter.getItem(i);
        String query = c.getString(c.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
        UserSearchView.setQuery(query, true);
        return true;
    }


    private void checkCartCount() {
        RelativeLayout relativeLayoutButton, relativeLayoutBadge;
        relativeLayoutButton = findViewById(R.id.layoutRelativeCheckoutButton);
        relativeLayoutBadge = findViewById(R.id.layoutRelativeButtonBadge);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);


        if (cartCount > 0) {
            ClearCartFab.setVisibility(View.VISIBLE);
            ClearCartFab.setClickable(true);
            layoutParams.setMargins(layoutParams.getMarginStart(), 0, layoutParams.getMarginEnd(), 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, R.layout.activity_user_menu);
            relativeLayoutButton.setLayoutParams(layoutParams);
            int margin = (deviceWidth / 8) - 40;

            RelativeLayout.LayoutParams layoutParamsBadge = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParamsBadge.setMargins(margin, 0, 0, 0);
            layoutParamsBadge.addRule(RelativeLayout.ALIGN_START, R.id.layoutLinearCheckoutButton);
            layoutParamsBadge.addRule(RelativeLayout.ALIGN_TOP, R.id.layoutLinearCheckoutButton);
            relativeLayoutBadge.setLayoutParams(layoutParamsBadge);
            relativeLayoutBadge.setVisibility(View.VISIBLE);
            TextView cartBadgeCount = findViewById(R.id.txtButtonBadge);
            cartBadgeCount.setText(String.valueOf(cartCount));
        } else {
            ClearCartFab.setVisibility(View.GONE);
            ClearCartFab.setClickable(false);

            int value = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -60.0f, getResources().getDisplayMetrics());
            layoutParams.setMargins(layoutParams.getMarginStart(), 0, layoutParams.getMarginEnd(), value);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, R.layout.activity_user_menu);
            relativeLayoutButton.setLayoutParams(layoutParams);
        }
    }


    private void generateRandomID() {

        String randomNumbers = null;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i <= 10; i++) {
            Random random = new Random();
            stringBuilder.append(random.nextInt(9));
            randomNumbers = stringBuilder.toString();
        }
        randomID = randomNumbers;
    }

    @Override
    public void onBackPressed() {
        sqLiteHelper.dataQuery("DELETE FROM productCartTable");
        cartCount = 0;
        finish();
    }

    private void checkoutDialog() {
        getCheckoutDetails();
        checkoutDialog.setCancelable(true);
        checkoutDialog.setCanceledOnTouchOutside(true);
        checkoutDialog.show();
    }

    private void getCheckoutDetails() {
        CartItemsList.clear();
        int productNetTotalPrice = 0;

        for (int i = 1; i <= sqLiteHelper.getCartCount(randomID); i++) {
            String cartId = randomID;
            int productId = sqLiteHelper.getCartProductId(i);
            int productQuantity = sqLiteHelper.getCartProductQuantity(i);
            int productPrice = sqLiteHelper.getProductPrice(productId);
            productNetTotalPrice += (productPrice * productQuantity);
            CartItemsList.add(new CartItems(cartId, productId, productQuantity));
        }



        CartNetTotal.setText(getResources().getString(R.string.txtNetTotalPricePrefix, productNetTotalPrice));
        CheckoutItemsListAdapter.notifyDataSetChanged();
    }

    private void getCheckoutNetTotal(){
        int productNetTotalPrice = 0;
        for (int i = 1; i <= sqLiteHelper.getCartCount(randomID); i++) {
            int productId = sqLiteHelper.getCartProductId(i);
            int productQuantity = sqLiteHelper.getCartProductQuantity(i);
            int productPrice = sqLiteHelper.getProductPrice(productId);
            productNetTotalPrice += (productPrice * productQuantity);
        }

        CartNetTotal.setText(getResources().getString(R.string.txtNetTotalPricePrefix, productNetTotalPrice));
    }

    private void createOrder(){
        if(cartCount >= 1){
            int arrayLength = CartItemsList.size();
            String[] productIdList = new String[arrayLength];
            String[] productQuantityList = new String[arrayLength];
            int productNetTotalPrice = 0;

            for(int i = 0; i < arrayLength; i++){
                int productId = CartItemsList.get(i).getProductId();
                int productQuantity = CartItemsList.get(i).getProductQuantity();
                int productPrice = sqLiteHelper.getProductPrice(productId);

                productIdList[i] = String.valueOf(productId);
                productQuantityList[i] = String.valueOf(productQuantity);
                productNetTotalPrice += (productPrice * productQuantity);
            }
            sqLiteHelper.createOrder(randomID, memberId, productIdList, productQuantityList, productNetTotalPrice, 0);
            Toast.makeText(this, "Order Number: " + randomID + " has been sent for processing!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "The Checkout cart is empty", Toast.LENGTH_SHORT).show();
        }
        checkoutDialog.cancel();
    }


    private void sortProductList(int value){

        Collections.sort(ProductList);

        if(value == 1){
            Collections.reverse(ProductList);
        }
        GridAdapter.notifyDataSetChanged();
        ListAdapter.notifyDataSetChanged();
    }
}
