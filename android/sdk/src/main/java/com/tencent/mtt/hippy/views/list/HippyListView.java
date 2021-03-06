/* Tencent is pleased to support the open source community by making Hippy available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.mtt.hippy.views.list;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.scroll.HippyScrollViewEventHelper;
import com.tencent.mtt.supportui.views.recyclerview.LinearLayoutManager;
import com.tencent.mtt.supportui.views.recyclerview.RecyclerView;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;

/**
 * Created by leonardgong on 2017/12/7 0007.
 */

public class HippyListView extends RecyclerView implements HippyViewBase
{
	private HippyListAdapter					mListAdapter;

	private Context								mContext;

	private HippyEngineContext					mHippyContext;

	private NativeGestureDispatcher				mGestureDispatcher;

	protected boolean							mScrollBeginDragEventEnable		= false;

	protected boolean							mScrollEndDragEventEnable		= false;

	protected boolean							mMomentumScrollBeginEventEnable	= false;

	protected boolean							mMomentumScrollEndEventEnable	= false;

	protected boolean							mScrollEventEnable				= true;

	protected boolean							mScrollEnable					= true;

	protected int								mScrollEventThrottle			= 400;  // 400ms最多回调一次
	private long								mLastScrollEventTimeStamp		= -1;

	private boolean								mHasRemovePreDraw				= false;
	private ViewTreeObserver.OnPreDrawListener	mPreDrawListener				= null;
	private ViewTreeObserver					mViewTreeObserver				= null;
	private OnInitialListReadyEvent				mOnInitialListReadyEvent;

	private OnScrollDragStartedEvent			mOnScrollDragStartedEvent;
	private OnScrollDragEndedEvent				mOnScrollDragEndedEvent;
	private OnScrollFlingStartedEvent			mOnScrollFlingStartedEvent;
	private OnScrollFlingEndedEvent				mOnScrollFlingEndedEvent;
	private OnScrollEvent						mOnScrollEvent;

	public HippyListView(Context context)
	{
		super(context);
		mHippyContext = ((HippyInstanceContext) context).getEngineContext();
		this.setLayoutManager(new LinearLayoutManager(context));
		mContext = context;
		setRepeatableSuspensionMode(false);
		mListAdapter = createAdapter(this, mHippyContext);
		setAdapter(mListAdapter);
	}

	protected HippyListAdapter createAdapter(RecyclerView hippyRecyclerView, HippyEngineContext hippyEngineContext)
	{
		return new HippyListAdapter(hippyRecyclerView, hippyEngineContext);
	}

	@Override
	public NativeGestureDispatcher getGestureDispatcher()
	{
		return mGestureDispatcher;
	}

	@Override
	public void setGestureDispatcher(NativeGestureDispatcher dispatcher)
	{
		this.mGestureDispatcher = dispatcher;
	}

	@Override
	public boolean onTouchEvent(MotionEvent motionEvent)
	{
		if (!mScrollEnable)
		{
			return false;
		}
		return super.onTouchEvent(motionEvent);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent motionEvent)
	{
		if (!mScrollEnable)
		{
			return false;
		}
		return super.onInterceptTouchEvent(motionEvent);
	}


	public void setListData()
	{
		LogUtils.d("hippylistview", "setListData");
		mListAdapter.notifyDataSetChanged();
		dispatchLayout();
	}

	public void setScrollBeginDragEventEnable(boolean enable)
	{
		mScrollBeginDragEventEnable = enable;
	}

	public void setScrollEndDragEventEnable(boolean enable)
	{
		mScrollEndDragEventEnable = enable;
	}

	public void setMomentumScrollBeginEventEnable(boolean enable)
	{
		mMomentumScrollBeginEventEnable = enable;
	}

	public void setMomentumScrollEndEventEnable(boolean enable)
	{
		mMomentumScrollEndEventEnable = enable;
	}

	public void setOnScrollEventEnable(boolean enable)
	{
		mScrollEventEnable = enable;
	}

	private HippyMap generateScrollEvent()
	{
		HippyMap contentOffset = new HippyMap();
		contentOffset.pushDouble("x", PixelUtil.px2dp(0));
		contentOffset.pushDouble("y", PixelUtil.px2dp(getOffsetY()));

		HippyMap event = new HippyMap();
		event.pushMap("contentOffset", contentOffset);

		return event;
	}

	public void setScrollEnable(boolean enable)
	{
		mScrollEnable = enable;
	}

	public void setScrollEventThrottle(int scrollEventThrottle)
	{
		mScrollEventThrottle = scrollEventThrottle;
	}

	@Override
	protected void onScrollDragStarted()
	{
		if (mScrollBeginDragEventEnable)
		{
			getOnScrollDragStartedEvent().send(this, generateScrollEvent());
		}
	}

	@Override
	protected void onScrollDragEnded()
	{
		if (mScrollEndDragEventEnable)
		{
			getOnScrollDragEndedEvent().send(this, generateScrollEvent());
		}
	}

	@Override
	protected void onScrollFlingStarted()
	{
		if (mMomentumScrollBeginEventEnable)
		{
			getOnScrollFlingStartedEvent().send(this, generateScrollEvent());
		}
	}

	@Override
	protected void onScrollFlingEnded()
	{
		if (mMomentumScrollEndEventEnable)
		{
			getOnScrollFlingEndedEvent().send(this, generateScrollEvent());
		}
	}

	@Override
	public void onScrolled(int x, int y)
	{
		super.onScrolled(x, y);
		sendOnScrollEvent();
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		if (!mHasRemovePreDraw)
		{
			mViewTreeObserver = getViewTreeObserver();
			if (mPreDrawListener == null)
			{
				mPreDrawListener = new ViewTreeObserver.OnPreDrawListener()
				{
					@Override
					public boolean onPreDraw()
					{
						if (mAdapter.getItemCount() > 0 && HippyListView.this.getChildCount() > 0)
						{
							mViewTreeObserver.removeOnPreDrawListener(this);
							mHasRemovePreDraw = true;

							post(new Runnable()
							{
								@Override
								public void run()
								{
									HippyListView.this.onInitialListReady();
									getOnInitialListReadyEvent().send(HippyListView.this, null);
								}
							});

						}
						return true;
					}
				};
			}
			mViewTreeObserver.removeOnPreDrawListener(mPreDrawListener);
			mViewTreeObserver.addOnPreDrawListener(mPreDrawListener);

		}
	}

	@Override
	protected void onDetachedFromWindow()
	{
		if (mPreDrawListener != null && mViewTreeObserver != null)
		{
			mViewTreeObserver.removeOnPreDrawListener(mPreDrawListener);
		}
		super.onDetachedFromWindow();
	}

	public void scrollToIndex(int xIndex, int yIndex, boolean animated,int duration)
	{
		if (animated)
		{
			int scrollToYPos = getHeightBefore(yIndex) - getOffsetY();
			if(duration != 0) //如果用户设置了duration
			{
				if (scrollToYPos != 0)
				{
					if (!mState.didStructureChange() )
					{
						mViewFlinger.smoothScrollBy(0, scrollToYPos, duration,true);
					}
				}
			}
			else
			{
				smoothScrollBy(0, scrollToYPos);
			}
		}
		else
		{
			scrollToPosition(yIndex, 0);
			post(new Runnable()
			{
				@Override
				public void run()
				{
					dispatchLayout();
				}
			});
		}
	}

	public void scrollToContentOffset(double xOffset, double yOffset, boolean animated,int duration)
	{
		int yOffsetInPixel = (int) PixelUtil.dp2px(yOffset);
		if (animated)
		{
			int scrollToYPos = yOffsetInPixel - getOffsetY();
			if(duration != 0) //如果用户设置了duration
			{
				if (scrollToYPos != 0)
				{
					if (!mState.didStructureChange() )
					{
						mViewFlinger.smoothScrollBy(0, scrollToYPos, duration,true);
					}
				}
			}
			else
			{
				smoothScrollBy(0, scrollToYPos);
			}
		}
		else
		{
			//			scrollToPosition(0, -yOffsetInPixel);
			scrollBy(0, yOffsetInPixel - getOffsetY());
			post(new Runnable()
			{
				@Override
				public void run()
				{
					dispatchLayout();
				}
			});
		}
	}

	protected void sendOnScrollEvent()
	{
		if (mScrollEventEnable)
		{
			long currTime = System.currentTimeMillis();
			if (currTime - mLastScrollEventTimeStamp < mScrollEventThrottle)
			{
				return;
			}

			mLastScrollEventTimeStamp = currTime;
			getOnScrollEvent().send(this, generateScrollEvent());
		}
	}

	// start drag event
	protected OnScrollDragStartedEvent getOnScrollDragStartedEvent()
	{
		if (mOnScrollDragStartedEvent == null)
		{
			mOnScrollDragStartedEvent = new OnScrollDragStartedEvent(HippyScrollViewEventHelper.EVENT_TYPE_BEGIN_DRAG);
		}
		return mOnScrollDragStartedEvent;
	}

	protected class OnScrollDragStartedEvent extends HippyViewEvent
	{
		public OnScrollDragStartedEvent(String eventName)
		{
			super(eventName);
		}
	}

	// end drag event
	protected OnScrollDragEndedEvent getOnScrollDragEndedEvent()
	{
		if (mOnScrollDragEndedEvent == null)
		{
			mOnScrollDragEndedEvent = new OnScrollDragEndedEvent(HippyScrollViewEventHelper.EVENT_TYPE_END_DRAG);
		}
		return mOnScrollDragEndedEvent;
	}

	protected class OnScrollDragEndedEvent extends HippyViewEvent
	{
		public OnScrollDragEndedEvent(String eventName)
		{
			super(eventName);
		}
	}

	// start fling
	protected OnScrollFlingStartedEvent getOnScrollFlingStartedEvent()
	{
		if (mOnScrollFlingStartedEvent == null)
		{
			mOnScrollFlingStartedEvent = new OnScrollFlingStartedEvent(HippyScrollViewEventHelper.EVENT_TYPE_MOMENTUM_BEGIN);
		}
		return mOnScrollFlingStartedEvent;
	}

	protected class OnScrollFlingStartedEvent extends HippyViewEvent
	{
		public OnScrollFlingStartedEvent(String eventName)
		{
			super(eventName);
		}
	}

	// end fling
	protected OnScrollFlingEndedEvent getOnScrollFlingEndedEvent()
	{
		if (mOnScrollFlingEndedEvent == null)
		{
			mOnScrollFlingEndedEvent = new OnScrollFlingEndedEvent(HippyScrollViewEventHelper.EVENT_TYPE_MOMENTUM_END);
		}
		return mOnScrollFlingEndedEvent;
	}

	protected class OnScrollFlingEndedEvent extends HippyViewEvent
	{
		public OnScrollFlingEndedEvent(String eventName)
		{
			super(eventName);
		}
	}

	// scroll
	protected OnScrollEvent getOnScrollEvent()
	{
		if (mOnScrollEvent == null)
		{
			mOnScrollEvent = new OnScrollEvent(HippyScrollViewEventHelper.EVENT_TYPE_SCROLL);
		}
		return mOnScrollEvent;
	}

	protected class OnScrollEvent extends HippyViewEvent
	{
		public OnScrollEvent(String eventName)
		{
			super(eventName);
		}
	}

	private OnInitialListReadyEvent getOnInitialListReadyEvent()
	{
		if (mOnInitialListReadyEvent == null)
		{
			mOnInitialListReadyEvent = new OnInitialListReadyEvent("initialListReady");
		}
		return mOnInitialListReadyEvent;
	}

	private class OnInitialListReadyEvent extends HippyViewEvent
	{
		public OnInitialListReadyEvent(String eventName)
		{
			super(eventName);
		}
	}

	protected void onInitialListReady()
	{

	}
}
