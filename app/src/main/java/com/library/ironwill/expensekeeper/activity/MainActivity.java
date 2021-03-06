package com.library.ironwill.expensekeeper.activity;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.PopupMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.jaeger.library.StatusBarUtil;
import com.library.ironwill.expensekeeper.App;
import com.library.ironwill.expensekeeper.BuildConfig;
import com.library.ironwill.expensekeeper.R;
import com.library.ironwill.expensekeeper.fragment.CardDetailFragment;
import com.library.ironwill.expensekeeper.fragment.CardListFragment;
import com.library.ironwill.expensekeeper.fragment.CardStatisticFragment;
import com.library.ironwill.expensekeeper.helper.TransitionHelper;
import com.library.ironwill.expensekeeper.util.BitmapUtil;
import com.library.ironwill.expensekeeper.view.ArcProgress.ArcProgress;
import com.library.ironwill.expensekeeper.view.DrawerItems.CustomPrimaryDrawerItem;
import com.library.ironwill.expensekeeper.view.DrawerItems.OverflowMenuDrawerItem;
import com.library.ironwill.expensekeeper.view.MaterialSpinner.MaterialSpinner;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import immortalz.me.library.TransitionsHeleper;
import immortalz.me.library.bean.InfoBean;
import immortalz.me.library.method.ColorShowMethod;

public class MainActivity extends TransitionHelper.BaseActivity implements DatePickerDialog.OnDateSetListener {

    protected static String BASE_FRAGMENT = "base_fragment";
    private MaterialMenuDrawable.IconState currentIconState;
    private ArcProgress mArcProgress;
    private TextView numIncome, numExpense;
    private MaterialSpinner mSpinner;
    private ImageView calImage;

    private MaterialMenuView homeButton;
    public View fragmentBackground;

    //Material DrawerLayout
    private IProfile profile, profile2, profile3, profile4, profile5;
    private AccountHeader headerResult = null;
    private static final int PROFILE_SETTING = 1;
    private Drawer drawer = null;

    private int per;

    private long exitTime = 0;
    private static Boolean isFirstLogin = true;
    private static Boolean stopFlag = false;
    private static Boolean mainFlag = true;
    private CardListFragment cardListFragment = null;

    //Camera
    String mCurrentPhotoPath;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;
    private static final int PICK_IMAGE_FROM_ALBUM = 100;
    private static final int PICK_IMAGE_FROM_CAMERA = 300;
    private static final int CROP_REQUEST_CODE = 400;

    private static final String[] dateList = {
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    };


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            cardListFragment = (CardListFragment) getSupportFragmentManager().getFragment(savedInstanceState, "ListFragment");
        } else {
            cardListFragment = new CardListFragment();
        }
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    PICK_IMAGE_FROM_CAMERA);
        }
        if (isFirstLogin) {
            TransitionsHeleper.getInstance()
                    .setShowMethod(new ColorShowMethod(R.color.white, R.color.endRed) {
                        @Override
                        public void loadCopyView(InfoBean bean, ImageView copyView) {
                            AnimatorSet set = new AnimatorSet();
                            set.playTogether(
                                    ObjectAnimator.ofFloat(copyView, "rotation", 0, 180),
                                    ObjectAnimator.ofFloat(copyView, "scaleX", 1, 0),
                                    ObjectAnimator.ofFloat(copyView, "scaleY", 1, 0)
                            );
                            set.setInterpolator(new AccelerateInterpolator());
                            set.setDuration(duration / 4 * 5).start();
                        }

                        @Override
                        public void loadTargetView(InfoBean bean, ImageView targetView) {

                        }
                    })
                    .show(this, null);
            isFirstLogin = false;
        }
        setContentView(R.layout.activity_main);
        StatusBarUtil.setTransparent(this);
        initView();
        initToolbar();
        initDrawer(savedInstanceState);

        int income = Integer.parseInt(numIncome.getText().toString().substring(1));
        int expense = Integer.parseInt(numExpense.getText().toString().substring(1));
        per = expense * 100 / income;
        if (!stopFlag) {
            progressBarHandler.post(updateProgress);
        } else {
            mArcProgress.setProgress(per);
        }
        initBaseFragment(savedInstanceState);
    }

    Handler progressBarHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mArcProgress.setProgress(msg.arg1);
            progressBarHandler.postDelayed(updateProgress, 33);
        }
    };

    Runnable updateProgress = new Runnable() {
        int i = 0;

        @Override
        public void run() {
            i += 1;
            Message msg = progressBarHandler.obtainMessage();
            msg.arg1 = i;
            if (i == per + 1) {
                progressBarHandler.removeCallbacks(updateProgress);
                stopFlag = true;
            } else {
                progressBarHandler.sendMessage(msg);
            }
        }
    };

    private void initDrawer(Bundle savedInstanceState) {
        profile = new ProfileDrawerItem().withName("Batman").withEmail("Bruce.Bat@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile));
        profile2 = new ProfileDrawerItem().withName("IronMan").withEmail("Stark.Iron@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile2)).withIdentifier(2);
        profile3 = new ProfileDrawerItem().withName("WonderWomen").withEmail("Diana.WW@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile3));
        profile4 = new ProfileDrawerItem().withName("Captain").withEmail("Steve.Cap@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile6)).withIdentifier(4);
        profile5 = new ProfileDrawerItem().withName("SuperMan").withEmail("Clark.Super@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile5));

        // Create the AccountHeader
        buildHeader(false, savedInstanceState);
        // Create the Drawer
        drawer = new DrawerBuilder()
                .withActivity(this)
//                .withToolbar(mToolbar)
                .withAccountHeader(headerResult) //set the AccountHeader we created earlier for the header
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.drawer_item_home).withIcon(FontAwesome.Icon.faw_home).withTextColor(getResources().getColor(R.color.almost_black)).withSelectedColor(getResources().getColor(R.color.middleBlue)),
                        new OverflowMenuDrawerItem().withName(R.string.drawer_item_menu_drawer_item).withMenu(R.menu.fragment_menu).withSelectedColor(getResources().getColor(R.color.middleBlue)).withOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                Toast.makeText(MainActivity.this, item.getTitle(), Toast.LENGTH_SHORT).show();
                                return false;
                            }
                        }).withIcon(GoogleMaterial.Icon.gmd_filter_center_focus).withTextColor(getResources().getColor(R.color.almost_black)).withSelectedColor(getResources().getColor(R.color.middleBlue)),
                        new CustomPrimaryDrawerItem().withName(R.string.drawer_item_manage).withIcon(FontAwesome.Icon.faw_amazon).withTextColor(getResources().getColor(R.color.almost_black)).withSelectedColor(getResources().getColor(R.color.middleBlue)),
                        new PrimaryDrawerItem().withName(R.string.drawer_item_custom).withIcon(FontAwesome.Icon.faw_eye).withTextColor(getResources().getColor(R.color.almost_black)).withSelectedColor(getResources().getColor(R.color.middleBlue)).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                            @Override
                            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                                int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                                if (mode == Configuration.UI_MODE_NIGHT_YES) {
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                                } else if (mode == Configuration.UI_MODE_NIGHT_NO) {
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                                }
                                getWindow().setWindowAnimations(R.style.WindowAnimFadeInOut);
                                recreate();
                                return true;
                            }
                        }),
//                        new CustomUrlPrimaryDrawerItem().withName(R.string.drawer_item_fragment_drawer).withDescription(R.string.drawer_item_fragment_drawer_desc).withIcon("https://avatars3.githubusercontent.com/u/1476232?v=3&s=460"),
                        new SectionDrawerItem().withName(R.string.drawer_item_section_header),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_settings).withIcon(FontAwesome.Icon.faw_cart_plus).withTextColor(getResources().getColor(R.color.almost_black)).withSelectedColor(getResources().getColor(R.color.middleBlue)),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_open_source).withIcon(FontAwesome.Icon.faw_github).withTextColor(getResources().getColor(R.color.almost_black)).withSelectedColor(getResources().getColor(R.color.middleBlue)).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                            @Override
                            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                                Intent mIntent = new Intent(Intent.ACTION_VIEW);
                                mIntent.setData(Uri.parse("https://github.com/IronWill23"));
                                startActivity(mIntent);
                                return true;
                            }
                        }),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_contact).withSelectedColor(getResources().getColor(R.color.middleBlue)).withIconTintingEnabled(true).withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_plus).actionBar().paddingDp(5).colorRes(R.color.material_drawer_dark_primary_text)).withTag("Bullhorn").withTextColor(getResources().getColor(R.color.almost_black)),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_help).withIcon(FontAwesome.Icon.faw_question).withEnabled(true).withTextColor(getResources().getColor(R.color.almost_black)).withSelectedColor(getResources().getColor(R.color.middleBlue))
                )
                .withOnDrawerNavigationListener(new Drawer.OnDrawerNavigationListener() {
                    @Override
                    public boolean onNavigationClickListener(View clickedView) {
                        //this method is only called if the Arrow icon is shown. The hamburger is automatically managed by the MaterialDrawer
                        MainActivity.this.finish();
                        return true;
                    }
                })
                /*.addStickyDrawerItems(
                        new SecondaryDrawerItem().withName(R.string.drawer_item_settings).withIcon(FontAwesome.Icon.faw_cog).withIdentifier(10),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_open_source).withIcon(FontAwesome.Icon.faw_github)
                )*/
                .withSavedInstance(savedInstanceState)
                .build();
    }

    private void buildHeader(boolean compact, Bundle savedInstanceState) {
        // Create the AccountHeader
        headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.top_background)
                .withCompactStyle(compact)
                .addProfiles(
                        profile,
                        profile2,
                        profile3,
                        profile4,
                        profile5,
                        //don't ask but google uses 14dp for the add account icon in Gmail but 20dp for the normal icons (like manage account)
                        new ProfileSettingDrawerItem().withName("Add Account").withDescription("Add new GitHub Account").withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_plus).actionBar().paddingDp(5).colorRes(R.color.material_drawer_dark_primary_text)).withIdentifier(PROFILE_SETTING),
                        new ProfileSettingDrawerItem().withName("Manage Account").withIcon(GoogleMaterial.Icon.gmd_settings)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        //sample usage of the onProfileChanged listener
                        if (profile instanceof IDrawerItem && profile.getIdentifier() == PROFILE_SETTING) {
                            IProfile newProfile = new ProfileDrawerItem().withNameShown(true).withName("Growlithe").withEmail("Growlithe@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile5));
                            if (headerResult.getProfiles() != null) {
                                //we know that there are 2 setting elements. set the new profile above them ;)
                                headerResult.addProfile(newProfile, headerResult.getProfiles().size() - 2);
                            } else {
                                headerResult.addProfiles(newProfile);
                            }
                        }
                        //false if you have not consumed the event and it should close the drawer
                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = drawer.saveInstanceState(outState);
        //add the values which need to be saved from the accountHeader to the bundle
        outState = headerResult.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initToolbar() {
        //setup the Action for Material Menu
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!drawer.isDrawerOpen()) {
                    if (homeButton.getState() == MaterialMenuDrawable.IconState.BURGER) {
                        drawer.openDrawer();
                    } else if (homeButton.getState() == MaterialMenuDrawable.IconState.X) {
                        homeButton.animateState(MaterialMenuDrawable.IconState.BURGER);
                    } else if (homeButton.getState() == MaterialMenuDrawable.IconState.ARROW) {
                        onBackPressed();
                    }
                } else {
                    drawer.closeDrawer();
                }
            }
        });

        //set up the spinner in toolbar
        //TODO Add a spinner animation
        Drawable mDrawable = getDrawable(R.drawable.spinner_background);
        mSpinner.setBackground(mDrawable);
        mSpinner.setTextColor(getResources().getColor(R.color.white));
        mSpinner.setArrowColor(getResources().getColor(R.color.white));
        mSpinner.setItems(dateList);
        mSpinner.setSelectedIndex(11);

        calImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (App.isCalendarIcon) {
                    Calendar now = Calendar.getInstance();
                    DatePickerDialog dpd = DatePickerDialog.newInstance(
                            MainActivity.this,
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH),
                            now.get(Calendar.DAY_OF_MONTH)
                    );
                    dpd.vibrate(false);
                    dpd.setVersion(DatePickerDialog.Version.VERSION_1);
                    dpd.show(getFragmentManager(), "DatePickDialog");
                } else {

                    String state = Environment.getExternalStorageState();
                    if (state.equals(Environment.MEDIA_MOUNTED)) {
                        Intent intent = new Intent();
                        // 指定开启系统相机的Action
                        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                        File outDir = Environment
                                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                        if (!outDir.exists()) {
                            outDir.mkdirs();
                        }
                        File outFile = new File(outDir, System.currentTimeMillis() + ".jpg");
                        // 把文件地址转换成Uri格式
                        Uri uri = null;
                        try {
                            uri = FileProvider.getUriForFile(MainActivity.this,
                                    BuildConfig.APPLICATION_ID + ".provider",
                                    createImageFile());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // 设置系统相机拍摄照片完成后图片文件的存放地址
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                        // 此值在最低质量最小文件尺寸时是0，在最高质量最大文件尺寸时是１
                        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
                        startActivityForResult(intent, PICK_IMAGE_FROM_CAMERA);
                    }
                }
            }
        });
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_FROM_CAMERA) {
            if (resultCode == RESULT_OK) {
            } else if (resultCode == RESULT_CANCELED) {
            } else {
            }
        }
    }

    private void initBaseFragment(Bundle savedInstanceState) {
        //apply background bitmap if we have one
        if (getIntent().hasExtra("bitmap_id")) {
            fragmentBackground.setBackground(new BitmapDrawable(getResources(), BitmapUtil.fetchBitmapFromIntent(getIntent())));
        }

        Fragment fragment = null;
        if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().findFragmentByTag(BASE_FRAGMENT);
        }
        if (fragment == null) fragment = getBaseFragment();
        setBaseFragment(fragment);
    }

    protected Fragment getBaseFragment() {
        int fragmentResourceId = getIntent().getIntExtra("fragment_resource_id", R.layout.fragment_card_list);
        switch (fragmentResourceId) {
            case R.layout.fragment_card_list:
            default:
                calImage.setImageResource(R.drawable.ic_calendar);
                App.isCalendarIcon = true;
                mainFlag = true;
                return new CardListFragment();
            case R.layout.fragment_card_detail:
                calImage.setImageResource(R.drawable.ic_camera);
                App.isCalendarIcon = false;
                mainFlag = false;
                return CardDetailFragment.create();
            case R.layout.fragment_statistic_detail:
                calImage.setImageResource(R.drawable.ic_calendar);
                App.isCalendarIcon = true;
                mainFlag = false;
                return CardStatisticFragment.create();
        }
    }

    public void setBaseFragment(Fragment fragment) {
        if (fragment == null) return;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.base_fragment, fragment, BASE_FRAGMENT);
        transaction.commit();
    }

    private void initView() {
        mArcProgress = (ArcProgress) findViewById(R.id.arc_progress);
        numIncome = (TextView) findViewById(R.id.num_income);
        numExpense = (TextView) findViewById(R.id.num_expense);
        homeButton = (MaterialMenuView) findViewById(R.id.material_menu_button);
        fragmentBackground = findViewById(R.id.base_fragment_background);
        mSpinner = (MaterialSpinner) findViewById(R.id.spinner_date);
        calImage = (ImageView) findViewById(R.id.calendar_pick);
    }

    public boolean animateHomeIcon(MaterialMenuDrawable.IconState iconState) {
        if (currentIconState == iconState) return false;
        currentIconState = iconState;
        homeButton.animateState(currentIconState);
        return true;
    }

    public void setHomeIcon(MaterialMenuDrawable.IconState iconState) {
        if (currentIconState == iconState) return;
        currentIconState = iconState;
        homeButton.setState(currentIconState);

    }

    public static MainActivity of(Activity activity) {
        return (MainActivity) activity;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0 && mainFlag) {
                this.exitApp();
            }else {
                return super.dispatchKeyEvent(event);
            }
        }
        return true;
    }

    private void exitApp() {
        // 判断2次点击事件时间
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(MainActivity.this, "Press Back again to exit", Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        String date = "You picked the following date: " + dayOfMonth + "/" + (++monthOfYear) + "/" + year;
    }
}
