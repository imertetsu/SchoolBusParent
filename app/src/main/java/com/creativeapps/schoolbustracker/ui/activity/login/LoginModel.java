package com.creativeapps.schoolbustracker.ui.activity.login;

import android.content.res.Resources;
import android.util.Log;

import com.creativeapps.schoolbustracker.R;
import com.creativeapps.schoolbustracker.data.DataManager;
import com.creativeapps.schoolbustracker.data.network.models.Parent;
import com.creativeapps.schoolbustracker.data.network.models.ParentResponse;
import com.creativeapps.schoolbustracker.data.network.services.ParentApiService;
import com.creativeapps.schoolbustracker.ui.activity.main.MainActivity;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginModel extends ViewModel {

    final String TAG = "LoginModel";

    //region Live data variables for EnterPhoneNumberFragment

    //boolean variable to indicate if the requested verification code received by the app or not
    private MutableLiveData<Boolean> mIsVerificationCodeReceived;
    //boolean variable to indicate if the process (request authentication code of the drive with
    // his telephone number) is running
    private MutableLiveData<Boolean> mIsWaitRespEnterMobile;
    //string variable that contains the response of the process (request authentication
    // code of the drive with his telephone number) from the server
    private MutableLiveData<String> mRespEnterMobile;
    //endregion

    //region Live data variables for ActivationCodeFragment

    //boolean variable to indicate if the process (verify authentication code of the parent)
    // is running
    private MutableLiveData<Boolean> mIsWaitRespVerifyParent;
    //string variable that contains the response of the process (verify authentication code of
    // the drive) from the server
    private MutableLiveData<String> mRespVerifyParent;
    //parent data
    private MutableLiveData<Parent> mParent;
    //endregion

    //country Code
    private MutableLiveData<String> mCountryCode;
    //mobile number
    private MutableLiveData<String> mMobileNumber;
    //parent Api service instance
    private ParentApiService mParentApiService;


    //region Getters and setters for the private variables defined above
    public MutableLiveData<Boolean> getIsVerificationCodeReceived() {
        return mIsVerificationCodeReceived;
    }

    public void setIsVerificationCodeReceived(Boolean isVerificationCodeReceived) {
        this.mIsVerificationCodeReceived.postValue(isVerificationCodeReceived);
    }

    public MutableLiveData<Boolean> getIsWaitRespEnterMobile() {
        return mIsWaitRespEnterMobile;
    }

    public void setIsWaitRespEnterMobile(Boolean isWaitRespEnterMobile) {
        this.mIsWaitRespEnterMobile.postValue(isWaitRespEnterMobile);
    }

    public MutableLiveData<String> getRespEnterMobile() {
        return mRespEnterMobile;
    }

    public void setRespEnterMobile(String respEnterMobile) {
        this.mRespEnterMobile.postValue(respEnterMobile);
    }

    public MutableLiveData<String> getCountryCode() {
        return mCountryCode;
    }

    public void setCountryCode(String countryCode) {
        this.mCountryCode.setValue(countryCode);
    }

    public MutableLiveData<String> getMobileNumber() {
        return mMobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mMobileNumber.setValue(mobileNumber);
    }

    public MutableLiveData<Parent> getParent() {
        return mParent;
    }

    public void setParent(Parent parent) {
        this.mParent.postValue(parent);
    }

    public MutableLiveData<Boolean> getIsWaitRespVerifyParent() {
        return mIsWaitRespVerifyParent;
    }

    public void setIsWaitRespVerifyParent(Boolean isWaitRespVerifyParent) {
        this.mIsWaitRespVerifyParent.postValue(isWaitRespVerifyParent);
    }

    public MutableLiveData<String> getRespVerifyParent() {
        return mRespVerifyParent;
    }

    public void setRespVerifyParent(String respVerifyParent) {
        this.mRespVerifyParent.postValue(respVerifyParent);
    }
    //endregion

    //region Constructor
    public LoginModel()
    {
        mIsVerificationCodeReceived = new MutableLiveData<>();
        mIsWaitRespEnterMobile = new MutableLiveData<>();
        mRespEnterMobile = new MutableLiveData<>();
        mIsWaitRespVerifyParent = new MutableLiveData<>();
        mRespVerifyParent = new MutableLiveData<>();
        mCountryCode = new MutableLiveData<>();
        mMobileNumber = new MutableLiveData<>();
        mParent = new MutableLiveData<>();
        mParentApiService = DataManager.getInstance().getParentApiService();
    }
    //endregion

    /*send verification code of the parent to the server for authentication*/
    public void sendVerificationCode(final String token, final String countryCode, final String mobileNumber, final String VerificationCode)

    //
    {
        //define a background thread
        Thread background = new Thread() {
            public void run() {
                //set RespVerifyParent to empty
                setRespVerifyParent("");
                //set IsWaitRespVerifyParent to true
                setIsWaitRespVerifyParent(true);
                try {
                    //call the verifyParent function to communicate with the server
                    //authenticateParent(countryCode, mobileNumber);
                    verifyParent(token, countryCode, mobileNumber, VerificationCode);
                    Log.d(TAG, "CountryCode: " +countryCode + ", mobileNumber: " +mobileNumber);

                } catch (Exception e) {
                    //if error, set IsWaitRespVerifyParent to false
                    setIsWaitRespVerifyParent(false);
                    Log.d(TAG, "run: " + e.getMessage());
                }
            }
        };
        //start the thread
        background.start();
    }

    /*request verification code of the parent from the server*/
    public void requestVerificationCode(final String countryCode, final String mobileNumber)
    {
        //set the country code and mobile number
        setCountryCode(countryCode);
        setMobileNumber(mobileNumber);
        //set RespEnterMobile to empty
        setRespEnterMobile("");
        //set IsWaitRespEnterMobile to true
        setIsWaitRespEnterMobile(true);

        //define a background thread
        Thread background = new Thread() {
            public void run() {
                try {
                    //call the authenticateParent function to communicate with the server
                    authenticateParent(countryCode, mobileNumber);
                } catch (Exception e) {
                    //if error, set IsWaitRespEnterMobile to false
                    setIsWaitRespEnterMobile(false);
                    Log.d(TAG, "run: " + e.getMessage());
                }
            }
        };
        background.start();
    }

    /*call the Api function authParentTelNumber and define a callback for this Api*/
    public void authenticateParent(String countryCode, String mobileNumber) {
        Call<ResponseBody> parentApiCall = mParentApiService.getParentApi().authParentTelNumber(countryCode, mobileNumber);
        //define the callback authenticateParentCallback to set the appropriate live data when
        // the Api function returns
        parentApiCall.enqueue(new authenticateParentCallback());
    }

    /*call the Api function verifyParentTelNumber and define a callback for this Api*/
   /* public void verifyParent(String token, String countryCode, String mobileNumber, String VerificationCode) {
        Call<ParentResponse> parentApiCall = mParentApiService.getParentApi().verifyParentTelNumber(token, countryCode, mobileNumber, VerificationCode);
        //define the callback verifyParentCallback to set the appropriate live data when
        // the Api function returns
        parentApiCall.enqueue(new verifyParentCallback());
    }*/

    public void verifyParent(String token, String countryCode, String mobileNumber, String VerificationCode) {
        Call<ParentResponse> parentApiCall = mParentApiService.getParentApi().verifyParentTelNumber(token, countryCode, mobileNumber, VerificationCode);
        //define the callback verifyParentCallback to set the appropriate live data when
        // the Api function returns
        parentApiCall.enqueue(new verifyParentCallback());
    }


    /*Callback for authenticateParent function*/
    private class authenticateParentCallback implements Callback<ResponseBody> {

        @Override
        public void onResponse( Call<ResponseBody> call, Response<ResponseBody> response) {
            //get the return code of the response
            int RetCode = response.code();
            //set IsWaitRespEnterMobile to false
            setIsWaitRespEnterMobile(false);
            switch (RetCode)
            {
                //return code is OK
                case 200:
                    //set the status RespEnterMobile to empty
                    setRespEnterMobile("");
                    //set IsVerificationCodeReceived to true
                    setIsVerificationCodeReceived(true);

                    break;
                //if the return code has error, set the status RespEnterMobile to an appropriate message
                case 404: //resource not found
                    setRespEnterMobile(LoginActivity.getContext().getString(R.string.tel_number_not_exist));
                    Log.d(TAG, response.message());
                    break;
                case 422: //validation error
                    setRespEnterMobile(LoginActivity.getContext().getString(R.string.error_in_tel));
                    Log.d(TAG, response.message());
                    break;
                case 500: //general server error
                default:
                    setRespEnterMobile(LoginActivity.getContext().getString(R.string.unexpected_error));
                    Log.d(TAG, response.message());
                    break;
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            //if failure in communication with the server
            setIsWaitRespEnterMobile(false);
            setRespEnterMobile(LoginActivity.getContext().getString(R.string.unable_to_connect_server));
        }
    }

    /*Callback for verifyParentTelNumber function*/
    private class verifyParentCallback implements Callback<ParentResponse> {

        @Override
        public void onResponse( Call<ParentResponse> call, Response<ParentResponse> response) {
            //get the return code of the response
            int RetCode = response.code();
            //set IsWaitRespEnterMobile to false
            setIsWaitRespVerifyParent(false);
            switch (RetCode)
            {
                //return code is OK
                case 200:
                    //set the status RespVerifyParent to empty
                    setRespVerifyParent("");
                    //extract the parent data from response and save it in the parent live data
                    ParentResponse r = response.body();
                    if(r!=null) {
                        setParent(r.getParent());
                    }
                    break;
                //if the return code has error, set the status RespVerifyParent to an appropriate message
                case 404: //resource not found
                    setRespVerifyParent(LoginActivity.getContext().getString(R.string.verification_code_not_correct));
                    Log.d(TAG, response.message());
                    break;
                case 422: //validation error
                    setRespVerifyParent(LoginActivity.getContext().getString(R.string.error_in_verification_code));
                    Log.d(TAG, response.message());
                    break;
                case 500: //general server error
                default:
                    setRespVerifyParent(LoginActivity.getContext().getString(R.string.unexpected_error));
                    Log.d(TAG, response.message());
                    break;
            }
        }

        @Override
        public void onFailure(Call<ParentResponse> call, Throwable t) {
            //if failure in communication with the server
            setIsWaitRespVerifyParent(false);
            setRespVerifyParent(LoginActivity.getContext().getString(R.string.unable_to_connect_server));
        }
    }

}
