package com.appgeneration.magmanager.inapp.abstractionlayer;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.appgeneration.magmanager.library.R;
import com.appgeneration.magmanager.model.ItemStatus;
import com.appgeneration.magmanager.util.TargetSettingsUtils;
import com.crashlytics.android.Crashlytics;
import com.sec.android.iap.sample.helper.SamsungIapHelper;
import com.sec.android.iap.sample.helper.SamsungIapHelper.OnGetInboxListListener;
import com.sec.android.iap.sample.helper.SamsungIapHelper.OnGetItemListListener;
import com.sec.android.iap.sample.helper.SamsungIapHelper.OnInitIapListener;
import com.sec.android.iap.sample.vo.InBoxVO;
import com.sec.android.iap.sample.vo.ItemVO;
import com.sec.android.iap.sample.vo.PurchaseVO;

public class InAppLayerSamsungImpl extends InAppLayer implements OnInitIapListener, OnGetItemListListener, OnGetInboxListListener {
	
	private static final String  TAG = InAppLayerSamsungImpl.class.getSimpleName();
	private SamsungIapHelper  mSamsungIapHelper  = null;
	private int mIapMode = SamsungIapHelper.IAP_MODE_COMMERCIAL;
	private String            mItemGroupId       = null;
	private WeakReference<Activity> mSourceActivityWeakReference = null;
	@Override
	public boolean initInAppPurchasing(Activity sourceActivity) throws InAppLayerException {
		Crashlytics.log("initializing inapp purchasing...");
		mItemGroupId = TargetSettingsUtils.IN_APP_SAMSUNG_ITEM_GROUP_ID;
		mSourceActivityWeakReference = new WeakReference<Activity>(sourceActivity);
		
		// ====================================================================
        
        // 2. SamsungIapHelper Instance 생성
        //    create SamsungIapHelper Instance
        //
        //    과금이 되지 않는 테스트 모드로 설정하고 싶다면, 
        //    SamsungIapHelper.IAP_MODE_TEST_SUCCESS 사용하세요.
        //    Billing does not want to set the test mode, 
        //    SamsungIapHelper.IAP_MODE_TEST_SUCCESS use.
        // ====================================================================
        mSamsungIapHelper = SamsungIapHelper.getInstance(sourceActivity, mIapMode );
        
        // 테스트를 위한 코드
        // For test...
        /*mSamsungIapHelper = new SamsungIapHelper( this ,
                                    SamsungIapHelper.IAP_MODE_TEST_SUCCESS );*/
        // ====================================================================
        
        // 3. OnInitIapListener 등록
        //    Register OnInitIapListener
        // ====================================================================
        mSamsungIapHelper.setOnInitIapListener( this );
        // ====================================================================
        
        // 4. OnGetItemListListener 등록
        //    Register OnGetItemListListener
        // ====================================================================
        mSamsungIapHelper.setOnGetItemListListener( this );
        // ====================================================================
        
        // 4.OnGetInboxListListener 등록
        //   Register OnGetInboxListListener
        // ====================================================================
        mSamsungIapHelper.setOnGetInboxListListener( this );
        // ====================================================================
        
        
        // 5. IAP 패키지가 설치되어 있다면.
        //    If IAP Package is installed in your device
        // ====================================================================
        if( true == mSamsungIapHelper.isInstalledIapPackage(sourceActivity) )
        {
            // 1) 설치된 패키지가 유효한 패키지라면
            //    If IAP package installed in your device is valid
            // ================================================================
            if( true == mSamsungIapHelper.isValidIapPackage(sourceActivity) )
            {
            	Crashlytics.log("inapp: getting item list..");
                // 서비스를 바인딩 하고 Item List 를 가져온다.
                // bind to IAPService and then get item list
                // ------------------------------------------------------------
                getItemListService();
                // ------------------------------------------------------------
                
            	 //mSamsungIapHelper.startAccountActivity( sourceActivity );
                
            }
            // ================================================================
            // 2) 설치된 패키지가 유효하지 않다면
            //    If IAP package installed in your device is not valid
            // ================================================================            
            else
            {
            	Crashlytics.log("inapp: detected invalid IAP package..");
                // show alert dialog for invalid IAP Package
                // ------------------------------------------------------------
                mSamsungIapHelper.showIapDialog(
                					sourceActivity,
                					sourceActivity.getString( R.string.in_app_purchase ),           
                					sourceActivity.getString( R.string.invalid_iap_package ),
                                     true,
                                     null );
                // ------------------------------------------------------------
            }
            // ================================================================ 
        }
        // 6. IAP 패키지가 설치되어 있지 않다면
        //    If IAP Package is not installed in your device
        // ====================================================================
        else
        {
        	Crashlytics.log("inapp: installing IAP package..");
            mSamsungIapHelper.installIapPackage( sourceActivity );
        }
        // ====================================================================
		return true;

	}
	
	/**
     * IAP Service 를 바이딩하고 정상적으로 바인딩 되었다면
     * safeGetItemListTask() 메소드를 호출하여 서버로부터 아이템 목록을 가져온다.
     * 
     * bind IAPService. If IAPService properly bound,
     * safeGetItemListTask() method is called to get item list.
     */
    public void getItemListService()
    {
        // 1. 서비스를 사용하기위해 Bind 처리를 한다.
        //    bind IAPService
        // ====================================================================
        mSamsungIapHelper.bindIapService( 
                                       new SamsungIapHelper.OnIapBindListener()
        {
            @Override
            public void onBindIapFinished( int result )
            {
                // 1) 서비스 바인드가 성공적으로 끝났을 경우 아이템 목록을 가져온다.
                //    If successfully bound IAPService
                // ============================================================
                if( result == SamsungIapHelper.IAP_RESPONSE_RESULT_OK )
                {
                    // Get item list
                    // --------------------------------------------------------
                    mSamsungIapHelper.safeGetItemList( mSourceActivityWeakReference.get(),
                                                       mItemGroupId,
                                                       1, Integer.MAX_VALUE, // Items from 1st to 15th
                                                       "01" );
                    // --------------------------------------------------------
                    
                    
                }
                // ============================================================
                // 2) 서비스 바인드가 성공적으로 끝나지 않았을 경우 에러메시지 출력
                //    If IAPService is not bound correctly
                // ============================================================
                else
                {
                    // dismiss ProgressDialog
                    // --------------------------------------------------------
                    //mSamsungIapHelper.dismissProgressDialog();
                    // --------------------------------------------------------
                	Crashlytics.log("inapp: failed to bind to iap service..");
                    // show alert dialog for bind failure
                    // --------------------------------------------------------
                    mSamsungIapHelper.showIapDialog(
                    		mSourceActivityWeakReference.get(),
                    		mSourceActivityWeakReference.get().getString( R.string.in_app_purchase ), 
                    		mSourceActivityWeakReference.get().getString( R.string.msg_iap_service_bind_failed ),
                             false,
                             null );
                    // --------------------------------------------------------
                }
                // ============================================================
            }
        });
        // ====================================================================
    }
    
    /**
     * Samsung Account 인증 결과와 IAP 결과 처리를 한다.
     * treat result of SamsungAccount Authentication and IAPService 
     */
    @Override
    public boolean handleActivityResult
    (   
        int     _requestCode,
        int     _resultCode,
        Intent  _intent
    )
    {
        switch ( _requestCode )
        {
            // 1. IAP 결제 결과 처리
            //    treat result of IAPService
            // ================================================================
            case SamsungIapHelper.REQUEST_CODE_IS_IAP_PAYMENT:
            {
                if( null == _intent )
                {
                    break;
                }
                
                Bundle extras         = _intent.getExtras();
                
                String itemId         = "";
                String thirdPartyName = "";

                // payment success   : 0
                // payment cancelled : 1
                // ============================================================
                int statusCode        = 1;
                // ============================================================
                
                String errorString    = "";
                PurchaseVO purchaseVO = null;
                
                // 1) IAP 에서 전달된 Bundle 정보가 존재할 경우
                //    If there is bundle passed from IAP
                // ------------------------------------------------------------
                if( null != extras )
                {
                    thirdPartyName = extras.getString(
                                  SamsungIapHelper.KEY_NAME_THIRD_PARTY_NAME );
                    
                    statusCode = extras.getInt( 
                                       SamsungIapHelper.KEY_NAME_STATUS_CODE );
                    
                    errorString = extras.getString( 
                                      SamsungIapHelper.KEY_NAME_ERROR_STRING );
                    
                    itemId = extras.getString(
                                           SamsungIapHelper.KEY_NAME_ITEM_ID );
                    
                    // 로그 출력 : 릴리즈 전에 삭제하세요.
                    // print log : Please remove before release
                    // --------------------------------------------------------
                    Log.i( TAG, "3rdParty Name : " + thirdPartyName + "\n" +
                                "ItemId        : " + itemId + "\n" +
                                "StatusCode    : " + statusCode + "\n" +
                                "errorString   : " + errorString );
                    // --------------------------------------------------------
                }
                // ------------------------------------------------------------
                // 2) IAP 에서 전달된 Bundle 정보가 존재하지 않는 경우
                //    If there is no bundle passed from IAP
                // ------------------------------------------------------------
                else
                {
                	Crashlytics.log("inapp.purchase: payment extras came as null..");
                    mSamsungIapHelper.showIapDialog(
                        mSourceActivityWeakReference.get(),
                        mSourceActivityWeakReference.get().getString( R.string.dlg_title_payment_error ), 
                        mSourceActivityWeakReference.get().getString( R.string.msg_payment_was_not_processed_successfully ),
                        false,
                        null );
                }
                // ------------------------------------------------------------
                // 3) 결제가 취소되지 않은 경우
                //    If payment was not cancelled
                // ------------------------------------------------------------
                if( Activity.RESULT_OK == _resultCode )
                {
                    // a. IAP 에서 넘어온 결제 결과가 성공인 경우 verifyurl 과 
                    //    purchaseId 값으로 서버에 해당 결제가 유효한 지 확인한다.
                    //    if Payment succeed
                    // --------------------------------------------------------
                    if( statusCode == SamsungIapHelper.IAP_ERROR_NONE )
                    {
//                        // 정상적으로 결제가 되었으므로 PurchaseVO를 생성한다.
//                        // make PurcahseVO
//                        // ----------------------------------------------------
                        purchaseVO = new PurchaseVO( extras.getString(
                                   SamsungIapHelper.KEY_NAME_RESULT_OBJECT ) );
//                        // ----------------------------------------------------
//                        
//                        // 결제 유효성을 확인한다.
//                        // verify payment result
//                        // ----------------------------------------------------
//                        mSamsungIapHelper.verifyPurchaseResult( mSourceActivityWeakReference.get(),
//                                                                purchaseVO );
//                        // ----------------------------------------------------
                        Crashlytics.log("inapp.purchase: user bought "+itemId+".");
                        Date d = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd",
                                                                     Locale.getDefault() );
                        String today = sdf.format( d );
                        mSamsungIapHelper.safeGetItemInboxTask( mSourceActivityWeakReference.get(), 
                                mItemGroupId,
                                1,
                                15,
                                "20130101",
                                today );
                    }
                    // --------------------------------------------------------
                    // b. IAP 에서 넘어온 결제 결과가 실패인 경우 에러메시지를 출력
                    //    Payment failed 
                    // --------------------------------------------------------
                    else
                    {
                        mSamsungIapHelper.showIapDialog(
                                 mSourceActivityWeakReference.get(),
                                 mSourceActivityWeakReference.get().getString( R.string.dlg_title_payment_error ),           
                                 errorString,
                                 false,
                                 null);

                    }
                    // --------------------------------------------------------
                }
                // ------------------------------------------------------------
                // 4) 결제가 취소된 경우
                //    If payment was cancelled
                // ------------------------------------------------------------
                else if( Activity.RESULT_CANCELED == _resultCode )
                {
                	Crashlytics.log("inapp.purchase: user cancelled buying "+itemId+".");
                    mSamsungIapHelper.showIapDialog(
                    		mSourceActivityWeakReference.get(),
                    		mSourceActivityWeakReference.get().getString( R.string.dlg_title_payment_cancelled ),
                    		mSourceActivityWeakReference.get().getString( R.string.dlg_msg_payment_cancelled ),
                             false,
                             null );
                }
                // ------------------------------------------------------------
                
                break;
            }
            // ================================================================
            
            // 2. 삼성 어카운트 계정 인증 결과 처리
            //    treat result of SamsungAccount authentication
            // ================================================================
            case SamsungIapHelper.REQUEST_CODE_IS_ACCOUNT_CERTIFICATION :
            {
                // 1) 삼성 계정 인증결과 성공인 경우
                //    If SamsungAccount authentication is succeed 
                // ------------------------------------------------------------
                if( Activity.RESULT_OK == _resultCode )
                {
                	Crashlytics.log("inapp.auth: samsung authentication succeeded.");
//                	Date d = new Date();
//                    SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd",
//                                                                 Locale.getDefault() );
//                    String today = sdf.format( d );
//                    mSamsungIapHelper.safeGetItemInboxTask( mSourceActivityWeakReference.get(), 
//                            mItemGroupId,
//                            1,
//                            15,
//                            "20130101",
//                            today );
                	
                	 // initialize IAPService.
                    // safeGetItemInboxList method is called after IAPService
                    // is initialized
                    // --------------------------------------------------------
                    mSamsungIapHelper.safeInitIap( mSourceActivityWeakReference.get() );
                    // --------------------------------------------------------
                }
                // ------------------------------------------------------------
                // 2) 삼성 계정 인증을 취소했을 경우
                //    If SamsungAccount authentication is cancelled
                // ------------------------------------------------------------
                else if( Activity.RESULT_CANCELED == _resultCode )
                {
                    // 프로그래스를 dismiss 처리합니다.
                    // dismiss ProgressDialog
                    // --------------------------------------------------------
                    mSamsungIapHelper.dismissProgressDialog();
                    // --------------------------------------------------------
                    Crashlytics.log("inapp.auth: samsung authentication cancelled.");
                    mSamsungIapHelper.showIapDialog(
                    		mSourceActivityWeakReference.get(),
                    		mSourceActivityWeakReference.get().getString( R.string.dlg_title_samsungaccount_authentication ),
                    		mSourceActivityWeakReference.get().getString( R.string.msg_authentication_has_been_cancelled ),
                             false,
                             null);
                }
                // ------------------------------------------------------------
                break;
            }
            // ================================================================
            
            default:
            {
            	return false;
            }
        }
		return true;
    }

	@Override
	public boolean startPurchaseProcessForItem(String itemId)
			throws InAppLayerException {
		Crashlytics.log("inapp.purchase: samsung authentication cancelled.");
		mSamsungIapHelper.startPurchase( 
                mSourceActivityWeakReference.get(), 
                SamsungIapHelper.REQUEST_CODE_IS_IAP_PAYMENT, 
                mItemGroupId,
                itemId );
		
		return true;
	}

	@Override
	public void onSucceedGetItemList(ArrayList<ItemVO> _itemList) {
		Crashlytics.log("inapp: onSucceedGetItemList: "+_itemList);
		mWereItemsLoaded = true;
		
		if (mItemList == null) {
			mItemList = new ArrayList<InAppLayerItem>();
		}
		mItemList.clear();
		
		for (ItemVO itemVO : _itemList) {
			mItemList.add(new InAppLayerItem(itemVO));
		}
		
		// 삼성 어카운트 계정 인증 진행
        // process SamsungAccount authentication
        // ------------------------------------------------------------
        mSamsungIapHelper.startAccountActivity( mSourceActivityWeakReference.get() );
        // ------------------------------------------------------------
	}

	@Override
	public void onSucceedInitIap() {
		Crashlytics.log("inapp: onSucceedInitIap");
		mSamsungIapHelper.dismissProgressDialog();
    	Date d = new Date();
      SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd",
                                                   Locale.getDefault() );
      String today = sdf.format( d );
      mSamsungIapHelper.safeGetItemInboxTask( mSourceActivityWeakReference.get(), 
              mItemGroupId,
              1,
              15,
              "20130101",
              today );
	}

	@Override
	public void dispose(){
		Crashlytics.log("inapp: disposing");
		if( null != mSamsungIapHelper )
        {
            mSamsungIapHelper.stopRunningTask();
            mSamsungIapHelper.dispose();
        }
	}

	@Override
	public void OnSucceedGetInboxList(ArrayList<InBoxVO> _inboxList) {
		Crashlytics.log("inapp: OnSucceedGetInboxList "+_inboxList);
		mSamsungIapHelper.dismissProgressDialog();
		for (InAppLayerItem item : mItemList) {
			for (InBoxVO inBoxVO : _inboxList) {
				if (item.getItemId().equals(inBoxVO.getItemId())) {
					item.setItemStatus(ItemStatus.BOUGHT);
				}
			}
			
		}
		
		if (getmInAppLayerListener() != null) {
			getmInAppLayerListener().onInAppLayerItemsStatusChanged();
		}
	}
}
