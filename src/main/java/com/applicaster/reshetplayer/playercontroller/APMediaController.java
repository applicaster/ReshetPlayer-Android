package com.applicaster.reshetplayer.playercontroller;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.lifecycle.LifecycleOwner;
import androidx.mediarouter.app.MediaRouteButton;

import com.applicaster.analytics.AnalyticsAgentUtil;
import com.applicaster.app.APProperties;
import com.applicaster.model.APAccount;
import com.applicaster.model.APModel;
import com.applicaster.player.Player;

import com.applicaster.player.controller.APSocialBarData;
import com.applicaster.player.controller.MidrollView;
import com.applicaster.player.wrappers.PlayerView;
import com.applicaster.plugin_manager.cast.CastPlugin;
import com.applicaster.plugin_manager.cast.ChromecastManager;
import com.applicaster.plugin_manager.playersmanager.Playable;
import com.applicaster.session.SessionStorage;
import com.applicaster.util.AppData;
import com.applicaster.util.FacebookUtil;
import com.applicaster.util.OSUtil;
import com.applicaster.util.PreferenceUtil;
import com.applicaster.util.StringUtil;
import com.applicaster.util.TwitterUtil;
import com.applicaster.util.TwitterUtil.TweetListener;
import com.applicaster.util.UrlSchemeUtil;
import com.applicaster.util.facebook.listeners.FBActionListener;
import com.applicaster.util.facebook.listeners.FBAuthoriziationListener;
import com.applicaster.util.facebook.model.FBModel;
import com.applicaster.util.facebook.permissions.APPermissionsType;
import com.applicaster.util.facebook.share.model.FBAction;
import com.applicaster.util.facebook.share.model.FBShare;
import com.applicaster.util.share.ShareUtils;
import com.applicaster.util.ui.APVideoViewWrapper;
import com.applicaster.util.ui.FacebookDrawer;
import com.applicaster.util.ui.ShareDialog;
import com.applicaster.util.ui.ShareDialog.ShareDialogListener;
import com.applicaster.util.ui.ShareDialog.ShareTypes;
import com.applicaster.zapp_automation.AutomationManager;
import com.applicaster.zapp_automation.CustomAccessibilityIdentifiers;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class APMediaController extends RelativeLayout implements APMediaControllerI {

	protected PlayerView player;
	protected Activity mContext;
	protected Playable playable;
	protected FBAction watchAction;
	
	protected View timerContainer,seekbarContainer;
	protected ImageView playPauseBtn,backwardBtn,forwardBtn, fullScreenBtn;
	protected TextView elapsedTime,totalTime;
	protected SeekBar seekbar;

	protected MidrollView midrollImageView;
	protected ImageView watch_checkbox, watch_close_button, watchButton;
	protected View watch_checkbox_label;
	protected boolean isLive;

	protected Timer hideTimer,currentPositionTimer,postWatchActionTimer;

	protected boolean seekingActionInProgress = false;
	public boolean chatButtonWasPressedForEnabling = true; // this field is for keeping the media controller for 3 more seconds when enabling chat

	// Socialbar
	protected static final String FB_WATCH_OPTED_IN = "userAllowedFBWatchAction";
	protected static final String WATCH_CHECKBOX_CANCELED = "watch_checkbox_canceled";
	public static final String IS_CHAT_ENABLED = "is_fb_chat_enabled";
	protected static final String FB_CHAT_FIRST_USE = "facebook_chat_first_use";

	// Share button. We use an Applicaster player plugin key as a flag
	private static final String APPLICASTER_PLAYER_PLUGIN_ID = "applicaster_video_player";
	private static final String SHARE_ENABLED_KEY = "share_enabled";

	protected boolean isSocialbarEnabled = false;
	protected boolean isScreenOffFeatureEnabled = false; // Determines whether or not to display the "Player Off" button in the Media Controller
	protected ImageView fbSharebtn,commentButton,recordBtn, screenOffButton, shareButton, subtitlesBtn;
	protected MediaRouteButton castBtn;
	protected View actionSelector;
	protected ImageView selectedAction;

	protected View captureDurationContainer;
	protected TextView captureDuration;

	// friends watching
	boolean isWatching;
	boolean isRecording = false;
	protected View watch_alert;


	protected PreferenceUtil prefUtil;	
	//	protected String fbObjectUrl;
	//	protected String fbObjectId;
	protected APSocialBarData socialData;

	protected TextView facebookFeedback;

	protected FullscreenCallback fullScreenCallback;


	private static final int CUSTOM_HIDE_TIME = 8000;
	private static final int SOCIAL_HIDE_TIME = 8000;
	private boolean isSubtitlesOn = false;

	public APMediaController(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = (Activity) context;
		prefUtil = PreferenceUtil.getInstance();
	}	

	@Override
	public void initView(){
		setSocialBarEnabledState();
		inflateLayout();
		initPlayPauseBtn();
		initRecordBtn();
		initShareBtn();
		initSeekBar();
		initPlayerOffButton();
		initWatchActionBtn();
		initCastButton();
		initFBShareBtns();
		initFullScreenBtns();
		setupAccessibilityIdentifiers();
	}

	/**
	 * This method is for the the subclass to override if needs to enable the default social media controller under different conditions
	 */
	protected void setSocialBarEnabledState(){
		isSocialbarEnabled = !isLive && AppData.getAPAccount().isSocialbarEnabled();
	}


	protected void setPlayerOffButtonEnabledState() {
		isScreenOffFeatureEnabled = isLive && AppData.getBooleanProperty(APProperties.PLAYER_OFF_FEATURE_ENABLED);
	}


	protected void inflateLayout() {
		if (isSocialbarEnabled ) {
			ViewGroup fbMediaController = (ViewGroup) findViewById(OSUtil.getResourceId("app_fb_media_controller"));
			//handles situations when staying in player and switching vod or channel inside the player, do not inflate if already inflated.
			if(fbMediaController == null){ 
				LayoutInflater.from(mContext).inflate(OSUtil.getLayoutResourceIdentifier("app_fb_media_controller"),this,true);
			}

		}
		else {
			ViewGroup apMediaController = (ViewGroup) findViewById(OSUtil.getResourceId("app_media_controller"));
			if(apMediaController == null){
				LayoutInflater.from(mContext).inflate(OSUtil.getLayoutResourceIdentifier("app_media_controller"),this,true);
			}

		}
	}

	protected void initPlayPauseBtn() {
		playPauseBtn = (ImageView) findViewById(OSUtil.getResourceId("play_pause_btn"));
		playPauseBtn.setOnClickListener(togglePlay);
	}

	protected void initRecordBtn(){
		recordBtn = (ImageView) findViewById(OSUtil.getResourceId("record_btn"));		
		if(recordBtn != null){
			recordBtn.setOnClickListener(toggleRecord);
			captureDurationContainer = findViewById(OSUtil.getResourceId("captureDurationContainer"));
			captureDuration = (TextView) findViewById(OSUtil.getResourceId("captureDuration")); 
		}
	}

	private void initShareBtn() {
		this.shareButton = findViewById(OSUtil.getResourceId("share_btn"));
		if (this.shareButton == null) {
			return;
		}

		String shareEnabledValue = SessionStorage.INSTANCE.get(SHARE_ENABLED_KEY, APPLICASTER_PLAYER_PLUGIN_ID);
		// compare with 0 and false, since Enabled is default, and we got "null" if the key is missing
		boolean shareDisabled = "0".equals(shareEnabledValue) || "false".equals(shareEnabledValue);
		if(shareDisabled){
			this.shareButton.setVisibility(GONE);
			return;
		}

		// Default share link would be app's public page if exists.
		String shareLink = APAccount.getPublicPageUrl( AppData.getProperty(APProperties.BROADCASTER_ID_KEY), APModel.PublicPageType.app );
		if (this.playable != null && StringUtil.isNotEmpty(playable.getPublicPageURL())) {
			shareLink = this.playable.getPublicPageURL();
		}

		if (StringUtil.isEmpty(shareLink)) {
			return;
		}

		final String finalShareLink = shareLink;
		final WeakReference<APMediaController> weakThis = new WeakReference<>(this);

		this.shareButton.setVisibility(VISIBLE);
		this.shareButton.setOnClickListener(v -> {
			APMediaController strongThis = weakThis.get();
			if(strongThis != null) {
				// Pause the player.
				strongThis.player.pause();

				// Share the item.
				ShareUtils.shareNativeForPlayable(strongThis.getContext(), strongThis.playable, finalShareLink);
			}
		});
	}

	/**
	 * set the subtitles default state base on the device captioning state.
	 */
	private void setSubtitlesState() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            CaptioningManager captioningManager = (CaptioningManager) getContext().getSystemService(Context.CAPTIONING_SERVICE);
            if(captioningManager.isEnabled()) {
				isSubtitlesOn = true;
			} else {
				isSubtitlesOn = false;
			}
		} else {
			isSubtitlesOn = false;
		}
	}

	/**
	 * init the subtitles button
	 */
	public void initSubtitlesBtn() {
		subtitlesBtn = (ImageView) findViewById(OSUtil.getResourceId("subtitles_btn"));
		if (subtitlesBtn != null) {
			subtitlesBtn.setVisibility(VISIBLE);
			if (player != null && player.isSubtitlesEnabled()) {
				setSubtitlesState();
				subtitlesBtn.setOnClickListener(toggleSubtitles);
			} else {
				subtitlesBtn.setVisibility(GONE);
			}
		}
		AutomationManager.getInstance().setAccessibilityIdentifier(subtitlesBtn, CustomAccessibilityIdentifiers.ApplicasterPlayerSubtitlesButton);
	}



	protected void initSeekBar() {
		timerContainer = findViewById(OSUtil.getResourceId("timer_container"));
		seekbarContainer = findViewById(OSUtil.getResourceId("seekbarContainer"));
		elapsedTime = (TextView) findViewById(OSUtil.getResourceId("time_elapsed"));
		//timeSeparator  = (TextView) findViewById(OSUtil.getResourceId("time_seperator"));
		totalTime  = (TextView) findViewById(OSUtil.getResourceId("total_time"));

		midrollImageView = (MidrollView)findViewById(OSUtil.getResourceId("midroll_image"));
		seekbar = (SeekBar) findViewById(OSUtil.getResourceId("seekbar"));
		seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				playerSeekTo(seekBar.getProgress()); 
				seekingActionInProgress = false;
				startCurrentPositionTimer();

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				seekingActionInProgress = true;
				player.onSeekStart(seekBar.getProgress());
				stopCurrentPositionTimer();
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser){
					String currentTime = StringUtil.parseDuration("" + progress);
					elapsedTime.setText(currentTime);
				}		
			}
		});
	}

	protected void initPlayerOffButton() {
		final View screenoffView = mContext.findViewById(OSUtil.getResourceId("screen_off_layout"));

		setPlayerOffButtonEnabledState(); // Set isScreenOffFeatureEnabled value

		screenOffButton = (ImageView) findViewById(OSUtil.getResourceId("player_off"));
		if (screenOffButton != null) { 
			screenOffButton.setVisibility(isScreenOffFeatureEnabled ? View.VISIBLE : View.GONE);
			screenOffButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if ((player.isPlaying())){
						pauseVideo();
						screenoffView.setVisibility(View.VISIBLE);
						screenOffButton.setImageDrawable(getResources().getDrawable(OSUtil.getDrawableResourceIdentifier("player_on_btn_selector")));
					}
					else {
						startVideo();
						screenoffView.setVisibility(View.GONE);
						screenOffButton.setImageDrawable(getResources().getDrawable(OSUtil.getDrawableResourceIdentifier("player_off_btn_selector")));
					}
				}
			});	
		}
	}
	
	
	protected void initFBShareBtns() {
		actionSelector = findViewById(OSUtil.getResourceId("action_selector"));
		if (isSocialbarEnabled){
			if(OSUtil.isSmallScreen(mContext)){
				fbSharebtn.setVisibility(View.GONE);
				findViewById(OSUtil.getResourceId("share_btn_padding")).setVisibility(View.GONE);
			}
			fbSharebtn = (ImageView) findViewById(OSUtil.getResourceId("share"));
			fbSharebtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					HashMap<String,String> params = new HashMap<String, String>(1);
					params.put(AnalyticsAgentUtil.FB_URL, socialData.objectUrl);
					AnalyticsAgentUtil.logEvent(AnalyticsAgentUtil.FB_SP_SHARE,params);

					postFeed();					
				}
			});


			initWatchDialog();

			selectedAction = (ImageView) findViewById(OSUtil.getResourceId("selected_action"));

			if(!isFbOptedIn()){
				selectedAction.setImageResource(OSUtil.getDrawableResourceIdentifier("fb_eye_off"));
			}			
			actionSelector.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					handleFbActionState();

				}
			});
			facebookFeedback = (TextView) mContext.findViewById(OSUtil.getResourceId("feedback"));
		}
		else{
 			if (actionSelector != null){
 				actionSelector.setVisibility(View.GONE);
 			}
 		}
		commentButton = (ImageView) findViewById(OSUtil.getResourceId("comment"));
		if(commentButton != null){
			commentButton.setOnClickListener( commentClickListener);
		}
	}

	private void initFullScreenBtns() {
		fullScreenBtn = (ImageView) findViewById(OSUtil.getResourceId("fullscreen"));
		if(fullScreenBtn != null) {
			fullScreenBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					fullScreenCallback.onGoToFullscreen(player.getCurrentPosition());
				}
			});
		}
	}

	public void setFullScreenCallback(FullscreenCallback fullScreenCallback) {
		this.fullScreenCallback = fullScreenCallback;
	}

	@Override
	public void setVolumeCallback(SetVolumeCallback setVolumeCallback) {

	}


	private void initWatchActionBtn() {

//		watchButton = (ImageView) findViewById(OSUtil.getResourceId("watch_action_button"));
//		watch_alert = findViewById(OSUtil.getResourceId("watch_alert"));
//
//		if(watchButton != null){
//			watchButton.setOnClickListener(new OnClickListener() {
//
//				@Override
//				public void onClick(View v) {
//					if(isFbOptedIn())
//					{
//						setFbOptInState(false);
//						watchButton.setImageDrawable(getResources().getDrawable(OSUtil.getDrawableResourceIdentifier("watch_action")));
//						deleteAction(watchAction);
//					}
//					else{
//						//handleFbAction will toggle the setFbOptInState = true
//						handleFbActionState() ;
//						watch_alert.setVisibility(View.GONE);
//						watchButton.setImageDrawable(getResources().getDrawable(OSUtil.getDrawableResourceIdentifier("watch_action_selected")));
//					}
//				}
//			});
//
//			if(isFbOptedIn())
//			{
//				watchButton.setImageDrawable(getResources().getDrawable(OSUtil.getDrawableResourceIdentifier("watch_action_selected")));
//				handleFbActionState() ;
//			}
//
//			initWatchDialog();
//
//		}

	}

	protected void initWatchDialog() {

		watch_checkbox = (ImageView) findViewById(OSUtil.getResourceId("watch_alert_checkbox"));
		watch_checkbox_label = findViewById(OSUtil.getResourceId("watch_checkbox_label"));
		watch_close_button = (ImageView) findViewById(OSUtil.getResourceId("watch_close_button"));

		if (watch_checkbox != null && watch_checkbox_label != null && watch_close_button != null){
			OnClickListener listener = new OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean old = prefUtil.getBooleanPref(WATCH_CHECKBOX_CANCELED, false);
					boolean newBool = !old;

					if(newBool){
						watch_checkbox.setImageResource(OSUtil.getDrawableResourceIdentifier("fb_filter_checkbox_selected"));
					}
					else{
						watch_checkbox.setImageResource(OSUtil.getDrawableResourceIdentifier("fb_filter_checkbox"));
					}
					prefUtil.setBooleanPref(WATCH_CHECKBOX_CANCELED, newBool);
				}
			};
			watch_checkbox.setOnClickListener(listener);
			watch_checkbox_label.setOnClickListener(listener);




			watch_close_button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					watch_alert.setVisibility(View.GONE);
				}
			});

			if(!prefUtil.getBooleanPref(WATCH_CHECKBOX_CANCELED, false)){
				watch_alert.setVisibility(View.VISIBLE);
			}
		}
	}

	protected void handleFbActionState() {
		if(!isFbOptedIn()){
			FacebookUtil.updateTokenIfNeeded(mContext, APPermissionsType.Player, new FBAuthoriziationListener() {
				@Override
				public void onError(Exception error) {

				}

				@Override
				public void onSuccess() {
					postAction(watchAction);
					//After the user choose to make "watch action" cancel the watch dialog.
					prefUtil.setBooleanPref(WATCH_CHECKBOX_CANCELED, true);
					if (watch_alert != null) {
						watch_alert.setVisibility(View.GONE);
					}

					setFbOptInState(true);
				}

				@Override
				public void onCancel() {

				}
			});
		}
		else{
			if (isSocialbarEnabled){
				deleteAction(watchAction);
			}
		}

	}

	protected LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(OSUtil.convertDPToPixels(50),OSUtil.convertDPToPixels(36));


	public void toggleMediaControllerState(){		
		if(this.getVisibility() == View.VISIBLE){

			hide();
		}
		else{

			show();
		}
	}

	public void show(){		
		if(isLive){
			if(timerContainer != null){
				timerContainer.setVisibility(View.GONE);
				seekbar.setVisibility(View.GONE);
			}
			if(seekbarContainer != null){
				seekbarContainer.setVisibility(View.GONE);
				seekbar.setVisibility(View.GONE);
			}
		}
		else{
			int currentPosition = getCurrentPosition();
			int duration = player.getDuration();
			if(!isSocialbarEnabled){
				seekbarContainer.setVisibility(View.VISIBLE);
			}
			seekbar.setVisibility(View.VISIBLE);
			seekbar.setMax(duration);
			seekbar.setProgress(currentPosition);
			String currentTime = StringUtil.parseDuration("" + currentPosition);
			elapsedTime.setText(currentTime);
			String totalTimeStr  = StringUtil.parseDuration("" + duration); 
			totalTime.setText(totalTimeStr); 

			((LayoutParams)seekbar.getLayoutParams()).topMargin = -1*seekbar.getThumbOffset()/2;
			startCurrentPositionTimer();
		}

		displayTopBarWithAnimation();

	}


	protected void displayTopBarWithAnimation() {
		this.setVisibility(View.VISIBLE);
		Animation animation = new TranslateAnimation(0, 0,  -100,0);
		animation.setDuration(300);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				APMediaController.this.setVisibility(View.VISIBLE);
				APMediaController.this.clearAnimation();

			}
		});
		animation.setFillAfter(true);
		this.startAnimation(animation);

		startHideTask(isSocialbarEnabled ? SOCIAL_HIDE_TIME : CUSTOM_HIDE_TIME);


		//		//mediaController and friendsWatching appears and disappear together if exist
		//		if(friendsWatchingView!= null && !FriendsWatchingView.isOpen){
		//			friendsWatchingView.showFriendsWatching(!FriendsWatchingView.isOpen, true);
		//		}

		setPlayPauseButtonState(player.isPlaying());

	}

	public void hide(){

		Animation animation = new TranslateAnimation(0, 0, 0,-100);
		animation.setDuration(300);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {


			}

			@Override
			public void onAnimationRepeat(Animation animation) {


			}

			@Override
			public void onAnimationEnd(Animation animation) {
				APMediaController.this.setVisibility(View.GONE);
				APMediaController.this.clearAnimation();

			}
		});
		animation.setFillAfter(true);
		this.setVisibility(View.VISIBLE);
		this.startAnimation(animation);
		if(hideTimer != null){
			try {
				hideTimer.cancel();
			} catch (Throwable t) {
			}

		}
		stopCurrentPositionTimer();

	}

	@Override
	public void setPlayer(PlayerView videoView) {
		this.player = videoView;
	}

	public void startCurrentPositionTimer(){
		currentPositionTimer = new Timer();
		currentPositionTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				mContext.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						int currentPosition =  getCurrentPosition();
						String currentTime = StringUtil.parseDuration("" + currentPosition);
						elapsedTime.setText(currentTime);
						if(recordBtn != null && captureDuration != null && captureDuration.getVisibility() == View.VISIBLE){
							Integer startPosition = (Integer) recordBtn.getTag();
							if(startPosition != null){
								captureDuration.setText(StringUtil.parseDuration("" + (currentPosition - startPosition)));
							}
						}
						if(!seekingActionInProgress){
							seekbar.setProgress(currentPosition);
						}
					}
				});

			}

		}, 200, 200);
	}

	public void stopCurrentPositionTimer(){
		if(currentPositionTimer != null){
			try {
				currentPositionTimer.cancel();
				currentPositionTimer = null;

			} catch (Throwable t) {
			}

		}
	}

	@Override
	public void setFBData(APSocialBarData socialData){
		this.socialData= socialData;
		watchAction = new FBAction ("", "", this.socialData.objectUrl, "");
		boolean showCommentsBtn = !StringUtil.isEmpty(socialData.fbObjectId);
		boolean showRecordBtn = socialData.isCaptureVideoEnabled && !isLive;
		boolean showChatBtn = socialData.isChatEnabled;

		if(commentButton != null){
			commentButton.setVisibility(showCommentsBtn ? View.VISIBLE : View.GONE);
		}
		if(recordBtn != null){
			recordBtn.setVisibility(showRecordBtn ? View.VISIBLE : View.GONE );
		}
		if(!isSocialbarEnabled){
			if(!showCommentsBtn && !showRecordBtn && !showChatBtn){
				findViewById(OSUtil.getResourceId("social_button_container")).setVisibility(View.GONE);
			}
			else if(!showCommentsBtn || !showRecordBtn || !showChatBtn){
				//findViewById(OSUtil.getResourceId("rightButtonsSeparator")).setVisibility(View.GONE);
			}
		}
		if(( watchButton!=null || isSocialbarEnabled ) && isFbOptedIn()){
			runPostWatchAction();
		}

	}


	protected void handleCustomChatBtnToggle(){
		//TODO
	}

	protected OnClickListener togglePlay = new OnClickListener() {

		@Override
		public void onClick(View v) {

			boolean isRemoteDevicePlaying = false;

			boolean isVideoPlaying = player.isPlaying() || isRemoteDevicePlaying;

			if (isVideoPlaying){
				pauseVideo();
				AnalyticsAgentUtil.logPauseButtonPressed();
			}
			else {
				AnalyticsAgentUtil.logResumeButtonPressed();
				startVideo();
			}
			//if the field isVideoPlaying = true
			//then now it's false because the player state was changed

			setPlayPauseButtonState(!isVideoPlaying);
		}
	};

	protected OnClickListener toggleSubtitles = new OnClickListener() {
		@Override
		public void onClick(View v) {

			if(isSubtitlesOn) {
				isSubtitlesOn = false;
				subtitlesOff();
			} else {
				isSubtitlesOn = true;
				subtitlesOn();
			}
		}
	};


	protected OnClickListener commentClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Make sure we have a valid
			FacebookUtil.updateTokenIfNeeded(mContext, APPermissionsType.Player, new FBAuthoriziationListener() {
				@Override
				public void onError(Exception error) {

				}

				@Override
				public void onSuccess() {
					openPostsBox();
				}

				@Override
				public void onCancel() {

				}
			});

		}
	};

	protected OnClickListener toggleRecord = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if(v.getTag() != null){
				isRecording = false;
				int startPosition = (Integer) v.getTag();
				int endPosition =  getCurrentPosition();
				String publicPageUrl = socialData.objectUrl + "?" + UrlSchemeUtil.addSnippestParams(startPosition,endPosition);
				String resName = isSocialbarEnabled ? "fb_player_record" : "player_record" ;
				((AnimationDrawable)recordBtn.getDrawable()).stop();
				recordBtn.setImageDrawable(mContext.getResources().getDrawable(OSUtil.getDrawableResourceIdentifier(resName)));
				AnalyticsAgentUtil.endTimedEvent(AnalyticsAgentUtil.CAPTURE_NUM_OF_CLICKS_BTN);
				captureDurationContainer.setVisibility(View.GONE);
				ShareDialog.showCaptureVideoShareDialog(mContext,publicPageUrl,socialData.itemName ,new ShareDialogListener() {

					@Override
					public void onSuccess(ShareTypes shareType) {
						if(ShareTypes.Facebook.equals(shareType)){
							//analyticsDetailsEvent(AnalyticsAgentUtil.CAPTURE_NUM_OF_FB_SHARES);
						}
						else if(ShareTypes.Twitter.equals(shareType)){
							//analyticsDetailsEvent(AnalyticsAgentUtil.CAPTURE_NUM_OF_TWITTER_SHARES);
						}
						else{
							//analyticsDetailsEvent(AnalyticsAgentUtil.CAPTURE_NUM_OF_MAILS_SHARES);
						}
					}

					@Override
					public void onError(Exception e, ShareTypes shareType) {

					}

					@Override
					public void onCancel() {
						startVideo();
					}
				});
				v.setTag(null);
				pauseVideo();
				toggleOnRecordControllerButtonsState(true);
				hide();
			}
			else{
				isRecording = true;
				int startPosition =  getCurrentPosition();
				if(!player.isPlaying()){
					// Start playing if is paused
					startVideo();
				}
				v.setTag(Integer.valueOf(startPosition));
				String resName = isSocialbarEnabled ? "fb_record_on_anim" : "player_record_on_anim" ;
				recordBtn.setImageDrawable(mContext.getResources().getDrawable(OSUtil.getDrawableResourceIdentifier(resName)));
				((AnimationDrawable)recordBtn.getDrawable()).start();
				toggleOnRecordControllerButtonsState(false);
				stopHideTask();
				captureDurationContainer.setVisibility(View.VISIBLE);
				captureDuration.setText(StringUtil.parseDuration("0"));

				Map<String,String> params = playable.getAnalyticsParams();
				AnalyticsAgentUtil.logTimedEvent(AnalyticsAgentUtil.CAPTURE_NUM_OF_CLICKS_BTN, params);
			}

		}
	};

	protected void toggleOnRecordControllerButtonsState(boolean enabled){
		if(playPauseBtn != null){
			playPauseBtn.setEnabled(enabled);
		}
		if(backwardBtn != null){
			backwardBtn.setEnabled(enabled);
		}
		if(forwardBtn != null){
			forwardBtn.setEnabled(enabled);
		}
		if(seekbar != null){
			seekbar.setEnabled(enabled);
		}
		if(fbSharebtn != null){
			fbSharebtn.setEnabled(enabled);
		}
		if(commentButton != null){
			commentButton.setEnabled(enabled);
		}
		if(actionSelector != null){
			actionSelector.setEnabled(enabled);
		}
	}
	protected OnClickListener handleBackward = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int currentPosition =  getCurrentPosition();
			currentPosition = Math.max(currentPosition - 5000, 0);
			playerSeekTo(currentPosition);
			startHideTask(2000);
		}
	};

	protected OnClickListener handleForward = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int currentPosition = getCurrentPosition();
			currentPosition = Math.min(currentPosition + 15000, player.getDuration());
			playerSeekTo(currentPosition);
			startHideTask(2000);
		}
	};

	private void setPlayPauseButtonState(boolean isVideoPlaying) {
		if (isVideoPlaying) {
			String resName = "ic_action_pause_large"  ;
			playPauseBtn.setImageDrawable(mContext.getResources().getDrawable(OSUtil.getDrawableResourceIdentifier(resName)));
			AutomationManager.getInstance().setAccessibilityIdentifier(playPauseBtn, CustomAccessibilityIdentifiers.ApplicasterPlayerPauseButton);
		}
		else {
			String resName = "ic_action_play_large" ;
			playPauseBtn.setImageDrawable(mContext.getResources().getDrawable(OSUtil.getDrawableResourceIdentifier(resName)));
			AutomationManager.getInstance().setAccessibilityIdentifier(playPauseBtn, CustomAccessibilityIdentifiers.ApplicasterPlayerPlayButton);
		}
	}

	public void setIsLive(boolean isLive) {
		this.isLive = isLive;

	}

	protected void startHideTask(int miliseconds){
		stopHideTask();
		hideTimer = new Timer();
		hideTimer.schedule(new HideTask(), miliseconds);

	}

	protected void stopHideTask(){
		if(hideTimer != null){
			hideTimer.cancel();
		}
	}

	protected class HideTask extends TimerTask{

		@Override
		public void run() {
			if(seekingActionInProgress){
				startHideTask(2000);
			}
			mContext.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					try {
						APMediaController.this.hide();
					} catch (Throwable t) {
					}

				}
			});
		}

	}

	protected void postFeed(){
		pauseVideo();
		FBShare post = new FBShare("", "", socialData.objectUrl, "");
		FacebookUtil.share(mContext, post, APPermissionsType.Player, new FBActionListener() {
			@Override
			public void onError(Exception error) {
				startVideo();
			}

			@Override
			public void onSuccess(FBModel model) {
				startVideo();
			}

			@Override
			public void onCancel() {
				startVideo();
			}
		});
	}

	//Send Analytics event
	protected void analyticsDetailsEvent(String eventName) {
		String title = socialData.itemName;
		String id = socialData.itemId;
		String showName  = socialData.itemShowName;
		HashMap<String, String> params = new HashMap<String, String>();
		if(isLive)
		{
			params.put("Channel Name", title);
		}
		else
		{
			params.put("Show name", showName);
			params.put("VOD title", title);
			params.put("ID", id);
		}
		AnalyticsAgentUtil.logEvent(eventName, params);

	}

	protected void shareTwitter(String defaultMessage) {
		//Analytics event
		String eventName = AnalyticsAgentUtil.TWITTER_BUTTON_CLICKED;
		analyticsDetailsEvent(eventName);

		pauseVideo();
		TwitterUtil.tweet(mContext, defaultMessage + "\n" + socialData.objectUrl,
				new TweetListener() {

					@Override
					public void onSuccess() {
						//Analytics event
						String eventName = AnalyticsAgentUtil.TWEET_SENT;
						analyticsDetailsEvent(eventName);

						startVideo();

					}

					@Override
					public void onError() {
						startVideo();

					}

					@Override
					public void onCancel() {
						startVideo();
			}
		});


	}


	//not in use for now
	protected void deleteAction(final FBShare action){
		if (action != null){
			if(isWatchActionPublished()){
//				FacebookUtil.deleteAction(mContext,watchAction.getActionID(),new AsyncTaskListener<Boolean>(){
//
//					@Override
//					public void handleException(Exception e) {
//
//					}
//
//					@Override
//					public void onTaskComplete(Boolean result) {
//						if(result){
//							((FBAction) action).setActionID(null);
//						}
//					}
//
//					@Override
//					public void onTaskStart() {
//
//					}
//				});

			}
		}
	}

	//not in use for now
	protected void postAction(final FBShare action){
		if(!isWatchActionPublished()){
//			FacebookUtil.doPostAction(mContext, action, APPermissionsType.OpenGraph , new FBActionListener(){
//
//				@Override
//				public void onError(Exception error) {
//
//				}
//
//				@Override
//				public void onSuccess(FBModel model) {
//					mIsWatchActionPublished = true;
//					watchAction.setActionID(model.getId());
//					HashMap<String,String> params = new HashMap<String, String>(1);
//					params.put(AnalyticsAgentUtil.FB_URL, socialData.objectUrl);
//					AnalyticsAgentUtil.logEvent(AnalyticsAgentUtil.FB_OPENGRAPH_WATCH, params);
//
//					showActionFeedback(((FBAction) action).getFeedbackMessage());
//				}
//
//				@Override
//				public void onCancel() {
//					setFbOptInState(false);
//				}
//			});
		}
	}

	private boolean isWatchActionPublished(){
		return  !StringUtil.isEmpty(watchAction.getActionID());
	}

	protected void showActionFeedback(String message){
		if(facebookFeedback == null){
			facebookFeedback = new TextView(mContext);
			LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.CENTER_IN_PARENT);
			facebookFeedback.setLayoutParams(params);
			RelativeLayout playerContainer = (RelativeLayout) mContext.findViewById(OSUtil.getResourceId("player_container"));
			facebookFeedback.setTextColor(0xffffffff);
			playerContainer.addView(facebookFeedback);
		}
		//		facebookFeedback = (TextView) mContext.findViewById(OSUtil.getResourceId("feedback"));
		facebookFeedback.setVisibility(View.VISIBLE);
		facebookFeedback.setText(message);
		ScaleAnimation anim = new ScaleAnimation(0,4,0,4,Animation.RELATIVE_TO_SELF, (float)0.5, Animation.RELATIVE_TO_SELF, (float)0.5);
		anim.setDuration(2000);
		anim.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				facebookFeedback.setVisibility(View.GONE);
				//				APMediaController.this.setVisibility(View.GONE);
			}
		});
		facebookFeedback.startAnimation(anim);
	}

	protected boolean isActionDropdownShown = false;
	private PostWatchActionTask postWatchActionTask;


	protected boolean isFbOptedIn(){
		return prefUtil.getBooleanPref(FB_WATCH_OPTED_IN, false);
	}

	protected void setFbOptInState(boolean optedIn){
		if (selectedAction != null){
			selectedAction.setImageResource(optedIn ? OSUtil.getDrawableResourceIdentifier("fb_eye_on") : OSUtil.getDrawableResourceIdentifier("fb_eye_off"));
		}
		prefUtil.setBooleanPref(FB_WATCH_OPTED_IN, optedIn);
	}



	protected void onDetachedFromWindow(){
		super.onDetachedFromWindow();
		Log.v("APMediaController", "onDetachedFromWindow");
		if(isSocialbarEnabled){
			cancelPostWatchAction();
			if(seekbar.getProgress()*2 < seekbar.getMax()){
				deleteAction(watchAction);
			}
		}

	}



	protected void runPostWatchAction(){
		postWatchActionTimer = new Timer();
		postWatchActionTask = new PostWatchActionTask();
		postWatchActionTimer.schedule(new PostWatchActionTask(), 10000);
	}

	protected void cancelPostWatchAction(){
		try {
			if(postWatchActionTimer != null){
				postWatchActionTimer.cancel();
				if(postWatchActionTask != null)
					postWatchActionTask.cancel();
			}
		} catch (Throwable e) {

		}
	}

	protected class PostWatchActionTask extends TimerTask{

		@Override
		public void run() {
			HashMap<String,String> params = new HashMap<String, String>(1);
			int durationInSeconds = player.getDuration()/1000;
			//params.put(WatchAction.EXPIRES_IN,"" + durationInSeconds);
			//watchAction.setExtraParams(params);
			postAction(watchAction);
		}

	}

	protected void openPostsBox(){
		FacebookDrawer.openPostsBox(mContext,socialData.fbObjectId);
		hide();
	}


	private void handleChatButtonClickedAnalytics(String param){
		HashMap<String,String> params = new HashMap<String, String>();
		params.put(AnalyticsAgentUtil.FB_CHAT_ID_PARAM, param);
		AnalyticsAgentUtil.logEvent(AnalyticsAgentUtil.FB_CHAT_CHAT_BUTTON_CLICKED, params);
	}



	protected void createScreenOffPlayerView() {

		final RelativeLayout screenOffImgContainer = new RelativeLayout(mContext);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);

		screenOffImgContainer.setLayoutParams(lp);
		screenOffImgContainer.setBackgroundColor(Color.parseColor("#e3e3e3"));
		screenOffImgContainer.setVisibility(GONE);

		ImageView screenOffImage = new ImageView(mContext);
		screenOffImage.setImageDrawable(mContext.getResources().getDrawable(OSUtil.getDrawableResourceIdentifier("screen_off_image")));

		LayoutParams screenOffImageParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		screenOffImageParams.addRule(CENTER_IN_PARENT);
		screenOffImage.setLayoutParams(screenOffImageParams);

		screenOffImgContainer.addView(screenOffImage);
		View webOverlay =  mContext.findViewById(OSUtil.getResourceId("web_overlay"));

		//add the screen off image below the overlay
		ViewGroup screenOffPlayerView = (ViewGroup) mContext.findViewById(OSUtil.getResourceId("player_container"));
		for (int i = 0; i < screenOffPlayerView.getChildCount(); i++) {
			if (screenOffPlayerView.getChildAt(i).equals(webOverlay)) {
				screenOffPlayerView.addView(screenOffImgContainer, i);
				break;
			}
		}
	}
	
	
	protected int getCurrentPosition(){
		int currentPosition = player.getCurrentPosition();
		return currentPosition;
	}
	
	private void playerSeekTo(int position){
			player.seekTo(position);
	}
	
	
	private void pauseVideo(){
			player.pause();
			player.onPause();
	}
	
	private void startVideo(){
			player.start();
			//player.onPlay();
	}
	
	

	private void subtitlesOn() {
		player.addSubtitles();
	}

	private void subtitlesOff() {
		player.removeSubtitles();

	}

	public boolean isUserRecoredVideo(){
		return isRecording;
	}

	private void disableTopBarButtons() {

		if(fbSharebtn != null){
			fbSharebtn.setEnabled(false);
		}
		if(commentButton != null){
			commentButton.setEnabled(false);
		}
		if(recordBtn != null){
			recordBtn.setEnabled(false);
		}
		if(screenOffButton != null){
			screenOffButton.setEnabled(false);;
		}
		if(watchButton != null){
			watchButton.setEnabled(false);
		}	
		if(seekbarContainer != null){
			seekbarContainer.setVisibility(GONE);
		}
		
		if(seekbar != null){
			seekbar.setVisibility(GONE);
		}
		
		if(timerContainer != null ){
			timerContainer.setVisibility(GONE);
		}
		
	}

	private void enableTopBarButtons() {

		if(fbSharebtn != null){
			fbSharebtn.setEnabled(true);
		}
		if(commentButton != null){
			commentButton.setEnabled(true);
		}
		if(recordBtn != null){
			recordBtn.setEnabled(true);
		}
		if(screenOffButton != null){
			screenOffButton.setEnabled(true);;
		}
		if(watchButton != null){
			watchButton.setEnabled(true);
		}	
		if(seekbarContainer != null && !isLive){
			seekbarContainer.setVisibility(VISIBLE);
		}
		
		if(seekbar != null && !isLive){
			seekbar.setVisibility(VISIBLE);
		}
		
		if(timerContainer !=null ){
			timerContainer.setVisibility(VISIBLE);
		}
		
	}

	@Override
	public void setPlayableItem(Playable playable) {
		// TODO Auto-generated method stub
		this.playable = playable;
	}

	@Override
	public void setDefaultVisibility() {
		setVisibility(View.GONE);
	}

	@Override
	public MidrollView getMidrollImageView() {
		return midrollImageView;
	}

	@Override
	public void initCastButton() {

		if (mContext instanceof Player) {

			Player player = (Player) mContext;

			castBtn = findViewById(OSUtil.getResourceId("media_route_button"));

			CastPlugin castPlugin = ChromecastManager.getInstance().getChromecastPlugin();
			if (castPlugin != null) {
				castPlugin.init(mContext, ((LifecycleOwner) mContext).getLifecycle(), ((Player) mContext).getPlayerConfig());

				// Check if there is a button defined on the player and a receiver id configured then initialize Google Cast
				if (castBtn != null && castPlugin.getReceiverAppId() != null) {
					castPlugin.initCastButton(castBtn, null);
				}
			}
		}
	}

    //region Automation

    private void setupAccessibilityIdentifiers() {
        AutomationManager.getInstance().setAccessibilityIdentifier(mContext.findViewById(android.R.id.content), CustomAccessibilityIdentifiers.ApplicasterPlayerScreenId);
		AutomationManager.getInstance().setAccessibilityIdentifier(castBtn, CustomAccessibilityIdentifiers.ApplicasterPlayerChromecastButton);
		AutomationManager.getInstance().setAccessibilityIdentifier(shareButton, CustomAccessibilityIdentifiers.ApplicasterPlayerNativeShareButton);
    }

    //endregion
}
