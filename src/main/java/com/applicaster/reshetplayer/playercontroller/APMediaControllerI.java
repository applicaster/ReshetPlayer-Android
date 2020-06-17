package com.applicaster.reshetplayer.playercontroller;

import com.applicaster.player.controller.APSocialBarData;
import com.applicaster.player.controller.MidrollView;
import com.applicaster.player.defaultplayer.gmf.layeredvideo.VideoPlayer;
import com.applicaster.player.wrappers.PlayerView;
import com.applicaster.plugin_manager.playersmanager.Playable;
import com.applicaster.util.ui.APVideoViewWrapper;

public interface APMediaControllerI {
	
	public void toggleMediaControllerState();
	public void show();
	public void hide();
	public void setPlayer(PlayerView videoView);
	public void setIsLive(boolean isLiveStream);
	public void setFBData(APSocialBarData socialData);
	public void initView();
	public void setPlayableItem(Playable playable);
	public void setDefaultVisibility();
	public void setFullScreenCallback(FullscreenCallback fullScreenCallback);
	public void setVolumeCallback(SetVolumeCallback setVolumeCallback);
	
	public MidrollView getMidrollImageView();

	public void initCastButton();

	void initSubtitlesBtn();

}
