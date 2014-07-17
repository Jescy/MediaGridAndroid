package com.dismantle.mediagrid;

import java.util.Date;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RTPullListView extends ListView implements OnScrollListener {  
	private static final String TAG = "RTPullListView";

	private final static int RELEASE_To_REFRESH = 0;
	private final static int PULL_To_REFRESH = 1;
	private final static int REFRESHING = 2;
	private final static int DONE = 3;
	private final static int LOADING = 4;

	private final static int RATIO = 3;

	private LayoutInflater mInflater;

	private LinearLayout mHeadView;

	private TextView mTVTips;
	private TextView mTVlastUpdated;
	private ImageView mImageViewArrow;
	private ProgressBar mProgressBar;

	private RotateAnimation mAnimation;
	private RotateAnimation mReverseAnimation;


	private boolean mIsRecored;

	private int mHeadContentHeight;

	private int mStartY;
	private int mFirstItemIndex;
	private int mState;
	private boolean mIsBack;
	private OnRefreshListener mRefreshListener;

	private boolean mIsRefreshable;
	private boolean mIsPush;

	private int mVisibleLastIndex;
	private int mVisibleItemCount;

	public RTPullListView(Context context) {
		super(context);
		init(context);
	}

	public RTPullListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	private void init(Context context) {
		mInflater = LayoutInflater.from(context);
		mHeadView = (LinearLayout) mInflater.inflate(R.layout.pulllistview_header, null);
		mImageViewArrow = (ImageView) mHeadView.findViewById(R.id.head_arrowImageView);
		mProgressBar = (ProgressBar) mHeadView.findViewById(R.id.head_progressBar);
		mTVTips = (TextView) mHeadView.findViewById(R.id.head_tipsTextView);
		mTVlastUpdated = (TextView) mHeadView.findViewById(R.id.head_lastUpdatedTextView);

		measureView(mHeadView);
		mHeadContentHeight = mHeadView.getMeasuredHeight();
		mHeadView.setPadding(0, -1 * mHeadContentHeight, 0, 0);
		mHeadView.invalidate();

		addHeaderView(mHeadView, null, false);
		setOnScrollListener(this);

		mAnimation = new RotateAnimation(0, -180,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mAnimation.setInterpolator(new LinearInterpolator());
		mAnimation.setDuration(250);
		mAnimation.setFillAfter(true);

		mReverseAnimation = new RotateAnimation(-180, 0,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mReverseAnimation.setInterpolator(new LinearInterpolator());
		mReverseAnimation.setDuration(200);
		mReverseAnimation.setFillAfter(true);

		mState = DONE;
		mIsRefreshable = false;
		mIsPush = true;
	}
	
	public void onScroll(AbsListView arg0, int firstVisiableItem, int arg2,
			int arg3) {
		mFirstItemIndex = firstVisiableItem;
		mVisibleLastIndex = firstVisiableItem + arg2 - 1; 
		mVisibleItemCount = arg2;
		if(mFirstItemIndex == 1 && !mIsPush){
			setSelection(0);
		}
	}
	
	public void setSelectionfoot(){
		this.setSelection(mVisibleLastIndex - mVisibleItemCount + 1);
	}

	public void onScrollStateChanged(AbsListView arg0, int arg1) {
		
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (mIsRefreshable) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (mFirstItemIndex == 0 && !mIsRecored) {
					mIsRecored = true;
					mIsPush = true;
					mStartY = (int) event.getY();
				}
				break;
			case MotionEvent.ACTION_UP:
				if (mState != REFRESHING && mState != LOADING) {
					if (mState == DONE) {
					}
					if (mState == PULL_To_REFRESH) {
						mState = DONE;
						changeHeaderViewByState();

					}
					if (mState == RELEASE_To_REFRESH) {
						mState = REFRESHING;
						changeHeaderViewByState();
						onRefresh();

					}
				}

				mIsRecored = false;
				mIsBack = false;

				break;

			case MotionEvent.ACTION_MOVE:
				int tempY = (int) event.getY();

				if (!mIsRecored && mFirstItemIndex == 0) {
					mIsRecored = true;
					mStartY = tempY;
				}

				if (mState != REFRESHING && mIsRecored && mState != LOADING) {

					// 保证在设置padding的过程中，当前的位置一直是在head，否则如果当列表超出屏幕的话，当在上推的时候，列表会同时进行滚动

					// 可以松手去刷新了
					if (mState == RELEASE_To_REFRESH) {

						setSelection(0);

						// 往上推了，推到了屏幕足够掩盖head的程度，但是还没有推到全部掩盖的地步
						if (((tempY - mStartY) / RATIO < mHeadContentHeight)
								&& (tempY - mStartY) > 0) {
							mState = PULL_To_REFRESH;
							changeHeaderViewByState();

						}
						// 一下子推到顶了
						else if (tempY - mStartY <= 0) {
							mState = DONE;
							changeHeaderViewByState();

						}
						// 往下拉了，或者还没有上推到屏幕顶部掩盖head的地步
						else {
							// 不用进行特别的操作，只用更新paddingTop的值就行了
						}
					}
					// 还没有到达显示松开刷新的时候,DONE或者是PULL_To_REFRESH状态
					if (mState == PULL_To_REFRESH) {

						setSelection(0);

						// 下拉到可以进入RELEASE_TO_REFRESH的状态
						if ((tempY - mStartY) / RATIO >= mHeadContentHeight) {
							mState = RELEASE_To_REFRESH;
							mIsBack = true;
							changeHeaderViewByState();
						}
						// 上推到顶了
						else if (tempY - mStartY <= 0) {
							mState = DONE;
							changeHeaderViewByState();
							mIsPush = false;
						}
					}

					// done状态下
					if (mState == DONE) {
						if (tempY - mStartY > 0) {
							mState = PULL_To_REFRESH;
							changeHeaderViewByState();
						}
					}

					// 更新headView的size
					if (mState == PULL_To_REFRESH) {
						mHeadView.setPadding(0, -1 * mHeadContentHeight
								+ (tempY - mStartY) / RATIO, 0, 0);

					}

					// 更新headView的paddingTop
					if (mState == RELEASE_To_REFRESH) {
						mHeadView.setPadding(0, (tempY - mStartY) / RATIO
								- mHeadContentHeight, 0, 0);
					}

				}

				break;
			}
		}

		return super.onTouchEvent(event);
	}

	// 当状态改变时候，调用该方法，以更新界面
	private void changeHeaderViewByState() {
		switch (mState) {
		case RELEASE_To_REFRESH:
			mImageViewArrow.setVisibility(View.VISIBLE);
			mProgressBar.setVisibility(View.GONE);
			mTVTips.setVisibility(View.VISIBLE);
			mTVlastUpdated.setVisibility(View.VISIBLE);

			mImageViewArrow.clearAnimation();
			mImageViewArrow.startAnimation(mAnimation);

			mTVTips.setText(getResources().getString(R.string.release_to_refresh));
			break;
		case PULL_To_REFRESH:
			mProgressBar.setVisibility(View.GONE);
			mTVTips.setVisibility(View.VISIBLE);
			mTVlastUpdated.setVisibility(View.VISIBLE);
			mImageViewArrow.clearAnimation();
			mImageViewArrow.setVisibility(View.VISIBLE);
			// 是由RELEASE_To_REFRESH状态转变来的
			if (mIsBack) {
				mIsBack = false;
				mImageViewArrow.clearAnimation();
				mImageViewArrow.startAnimation(mReverseAnimation);

				mTVTips.setText(getResources().getString(R.string.pull_to_refresh));
			} else {
				mTVTips.setText(getResources().getString(R.string.pull_to_refresh));
			}
			break;

		case REFRESHING:

			mHeadView.setPadding(0, 0, 0, 0);

			mProgressBar.setVisibility(View.VISIBLE);
			mImageViewArrow.clearAnimation();
			mImageViewArrow.setVisibility(View.GONE);
			mTVTips.setText(getResources().getString(R.string.refreshing));
			mTVlastUpdated.setVisibility(View.VISIBLE);

			break;
		case DONE:
			mHeadView.setPadding(0, -1 * mHeadContentHeight, 0, 0);

			mProgressBar.setVisibility(View.GONE);
			mImageViewArrow.clearAnimation();
			mImageViewArrow.setImageResource(R.drawable.pulltorefresh);
			mTVTips.setText(getResources().getString(R.string.pull_to_refresh));
			mTVlastUpdated.setVisibility(View.VISIBLE);

			break;
		}
	}

	public void setonRefreshListener(OnRefreshListener refreshListener) {
		this.mRefreshListener = refreshListener;
		mIsRefreshable = true;
	}

	public interface OnRefreshListener {
		public void onRefresh();
	}

	public void onRefreshComplete() {
		mState = DONE;
		CharSequence dateTime = DateFormat.format(getResources().getString(R.string.date_format), new Date());
		mTVlastUpdated.setText(getResources().getString(R.string.updating) + dateTime);
		changeHeaderViewByState();
		invalidateViews();
		setSelection(0);
	}

	private void onRefresh() {
		if (mRefreshListener != null) {
			mRefreshListener.onRefresh();
		}
	}
	
	public void clickToRefresh(){
		mState = REFRESHING;
		changeHeaderViewByState();
	}
	
	
	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,
					MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0,
					MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	public void setAdapter(BaseAdapter adapter) {
		mTVlastUpdated.setText(getResources().getString(R.string.updating) + new Date().toLocaleString());
		super.setAdapter(adapter);
	}
}  