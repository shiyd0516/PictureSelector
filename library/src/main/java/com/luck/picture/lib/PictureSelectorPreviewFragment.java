package com.luck.picture.lib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.luck.picture.lib.adapter.PicturePreviewAdapter;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.decoration.ViewPage2ItemDecoration;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.loader.LocalMediaLoader;
import com.luck.picture.lib.loader.LocalMediaPageLoader;
import com.luck.picture.lib.manager.SelectedManager;
import com.luck.picture.lib.utils.ActivityCompatHelper;
import com.luck.picture.lib.utils.DensityUtil;
import com.luck.picture.lib.widget.BottomNavBar;
import com.luck.picture.lib.widget.PreviewBottomNavBar;
import com.luck.picture.lib.widget.PreviewTitleBar;
import com.luck.picture.lib.widget.TitleBar;

import java.util.List;
import java.util.Objects;

/**
 * @author：luck
 * @date：2021/11/18 10:13 下午
 * @describe：PictureSelectorPreviewFragment
 */
public class PictureSelectorPreviewFragment extends PictureCommonFragment {
    public static final String TAG = PictureSelectorPreviewFragment.class.getSimpleName();

    public static PictureSelectorPreviewFragment newInstance() {
        return new PictureSelectorPreviewFragment();
    }

    private List<LocalMedia> mData;

    private PreviewTitleBar titleBar;

    private PreviewBottomNavBar bottomNarBar;

    private ViewPager2 viewPager;

    private PicturePreviewAdapter viewPageAdapter;

    private int curPosition;

    private int totalNum;

    private int screenWidth;

    private boolean isTransformPage = false;

    public void setData(int position, int totalNum, List<LocalMedia> data) {
        this.mData = data;
        this.totalNum = totalNum;
        this.curPosition = position;
    }

    @Override
    public int getResourceId() {
        return R.layout.ps_fragment_preview;
    }

    @Override
    public void onSelectedChange(boolean isAddRemove, LocalMedia currentMedia) {
        titleBar.getSelectedView().setSelected(SelectedManager.getSelectedResult().contains(currentMedia));
        bottomNarBar.setSelectedChange();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        screenWidth = DensityUtil.getScreenWidth(getContext());
        titleBar = view.findViewById(R.id.title_bar);
        viewPager = view.findViewById(R.id.preview_pager);
        bottomNarBar = view.findViewById(R.id.bottom_nar_bar);
        initLoader();
        initTitleBar();
        if (config.isExternalPreview) {
            bottomNarBar.setVisibility(View.GONE);
        } else {
            initBottomNavBar();
        }
        initViewPager();
    }

    /**
     * init LocalMedia Loader
     */
    protected void initLoader() {
        if (config.isPageStrategy) {
            mLoader = new LocalMediaPageLoader(getContext(), config);
        } else {
            mLoader = new LocalMediaLoader(getContext(), config);
        }
    }

    private void initTitleBar() {
        titleBar.setTitleBarStyle();
        titleBar.setOnTitleBarListener(new TitleBar.OnTitleBarListener() {
            @Override
            public void onBackPressed() {
                if (config.isExternalPreview) {
                    handleExternalPreviewBack();
                } else {
                    iBridgePictureBehavior.onFinish();
                }
            }
        });
        titleBar.setTitle((curPosition + 1) + "/" + totalNum);
        titleBar.getSelectedClickView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (config.isExternalPreview) {
                    handleExternalPreview();
                } else {
                    LocalMedia currentMedia = mData.get(viewPager.getCurrentItem());
                    int resultCode = confirmSelect(currentMedia, titleBar.getSelectedView().isSelected());
                    if (resultCode == SelectedManager.ADD_SUCCESS) {
                        titleBar.getSelectedView().startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.ps_anim_modal_in));
                    }
                }
            }
        });
    }

    /**
     * 调用了startPreview预览逻辑
     */
    @SuppressLint("NotifyDataSetChanged")
    private void handleExternalPreview() {
        if (PictureSelectionConfig.previewEventListener != null) {
            PictureSelectionConfig.previewEventListener.onPreviewDelete(viewPager.getCurrentItem());
            int currentItem = viewPager.getCurrentItem();
            mData.remove(currentItem);
            if (mData.size() == 0) {
                if (!ActivityCompatHelper.isDestroy(getActivity())) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
                return;
            }
            titleBar.setTitle(getString(R.string.picture_preview_image_num,
                    curPosition + 1, mData.size()));
            totalNum = mData.size();
            curPosition = currentItem;
            viewPager.setCurrentItem(curPosition, false);
            isTransformPage = true;
            viewPager.setPageTransformer(new ViewPager2.PageTransformer() {
                @Override
                public void transformPage(@NonNull View page, float position) {
                    if (isTransformPage) {
                        ObjectAnimator animator = ObjectAnimator.ofFloat(page, "alpha", 0F, 1F);
                        animator.setDuration(450);
                        animator.setInterpolator(new LinearInterpolator());
                        animator.start();
                        isTransformPage = false;
                    }
                }
            });
            viewPageAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 处理外部预览返回处理
     */
    private void handleExternalPreviewBack() {
        if (!ActivityCompatHelper.isDestroy(getActivity())) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }


    private void initBottomNavBar() {
        bottomNarBar.setBottomNavBarStyle();
        bottomNarBar.setSelectedChange();
        bottomNarBar.setOnBottomNavBarListener(new BottomNavBar.OnBottomNavBarListener() {

            @Override
            public void onComplete() {
                dispatchTransformResult();
            }

            @Override
            public void onEditImage() {

            }
        });
    }

    private void initViewPager() {
        viewPageAdapter = new PicturePreviewAdapter(getContext(), mData, config);
        viewPageAdapter.setOnPreviewEventListener(new PicturePreviewAdapter.OnPreviewEventListener() {
            @Override
            public void onBackPressed() {
                if (config.isExternalPreview) {
                    handleExternalPreview();
                } else {
                    iBridgePictureBehavior.onFinish();
                }
            }

            @Override
            public void onPreviewVideoTitle(String videoName) {
                if (TextUtils.isEmpty(videoName)) {
                    titleBar.setTitle((curPosition + 1) + "/" + totalNum);
                } else {
                    titleBar.setTitle(videoName);
                }
            }
        });
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager.addItemDecoration(new ViewPage2ItemDecoration(1,
                DensityUtil.dip2px(Objects.requireNonNull(getActivity()), 1)));
        viewPager.setAdapter(viewPageAdapter);
        viewPager.setCurrentItem(curPosition, false);
        titleBar.getSelectedView().setSelected(SelectedManager.getSelectedResult().contains(mData.get(viewPager.getCurrentItem())));
        viewPager.registerOnPageChangeCallback(pageChangeCallback);
    }

    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            LocalMedia media = positionOffsetPixels < screenWidth / 2 ? mData.get(position) : mData.get(position + 1);
            titleBar.getSelectedView().setSelected(isSelected(media));
        }

        @Override
        public void onPageSelected(int position) {
            curPosition = position;
            titleBar.setTitle((curPosition + 1) + "/" + totalNum);
        }
    };

    /**
     * 当前图片是否选中
     *
     * @param media
     * @return
     */
    protected boolean isSelected(LocalMedia media) {
        return SelectedManager.getSelectedResult().contains(media);
    }

    @Override
    public void onDestroy() {
        viewPageAdapter.destroyCurrentVideoHolder();
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        super.onDestroy();
    }
}