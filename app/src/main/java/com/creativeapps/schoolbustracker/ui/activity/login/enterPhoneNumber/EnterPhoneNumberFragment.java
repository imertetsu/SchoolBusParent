package com.creativeapps.schoolbustracker.ui.activity.login.enterPhoneNumber;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.creativeapps.schoolbustracker.R;
import com.creativeapps.schoolbustracker.ui.activity.login.LoginActivity;
import com.creativeapps.schoolbustracker.ui.activity.login.LoginModel;
import com.rilixtech.widget.countrycodepicker.CountryCodePicker;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class EnterPhoneNumberFragment extends Fragment implements View.OnClickListener {

    final String TAG = "EnterPhoneNumberFrag";

    //Vista modelo para la activadad
    private LoginModel mViewModel;
    //Edit text usado para ingresar el nro de telefono del padre
    private EditText mPhoneNumberEdt;
    //spinner que se utiliza para indicar un proceso de larga ejecución, como la comunicación con el backend
    private ProgressBar mSpinner;
    //superposición que evita que el usuario interactúe con cualquier elemento de la interfaz gráfica de usuario en la pantalla se muestra el spinner
    private Dialog mOverlayDialog;
    //Text view que muestran el estado del proceso de inicio de sesión después de ingresar el número de teléfono
    private TextView mStatus;
    //selector de código de país
    private CountryCodePicker mCountryCodePicker;
    //navigation controller utilizado para navegar entre fragmentos en esta actividad
    private NavController mNavController;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //inflate the layout
        View view = inflater.inflate(R.layout.fragment_enter_phone_number, container, false);
        //crea el modelo de la vista para esta actividad
        mViewModel = ((LoginActivity)getActivity()).createViewModel();

        return view;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //inicializa todos los elementos de la interfaz (gui) despues de que la vista es creada

        mOverlayDialog = new Dialog(this.getContext(), android.R.style.Theme_Panel);
        mSpinner =view.findViewById(R.id.MobNumberProgressBar);
        mSpinner.setVisibility(View.GONE);

        mStatus =view.findViewById(R.id.MobNumberStatus);

        Button nextBtn = view.findViewById(R.id.NextBtn);
        nextBtn.setOnClickListener(this);

        mCountryCodePicker = view.findViewById(R.id.ccp);
        mPhoneNumberEdt = view.findViewById(R.id.PhoneNumberEdt);
        mCountryCodePicker.registerPhoneNumberTextView(mPhoneNumberEdt);

        mNavController = Navigation.findNavController(EnterPhoneNumberFragment.this.getActivity(),
                R.id.nav_host_fragment_login);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //empieza a observar los datos en vivo del modelo de la vista los cuales son

        //boolean IsVerificationCodeReceived que indica si el codigo de verificacion requerido fue recibido por la app o no
        mViewModel.getIsVerificationCodeReceived().observe(this, new VerificationCodeReceivedObserver());
        //boolean IsWaitRespEnterMobile indica si el proceso (request authentication
        // code of the drive with his telephone number) esta corriendo
        mViewModel.getIsWaitRespEnterMobile().observe(this, new LoadingObserver());
        //string RespEnterMobile que es la respuesta del proceso (request authentication
        // code of the drive with his telephone number) del servidor
        mViewModel.getRespEnterMobile().observe(this, new statusObserver());
        //string CountryCode
        mViewModel.getCountryCode().observe(this, new CountryCodeObserver());
    }

    @Override
    public void onPause()
    {
        super.onPause();
        //para de observar los datos en vivo cuando el fragmento es pauseado
        mViewModel.getIsVerificationCodeReceived().removeObservers(this);
        mViewModel.getIsWaitRespEnterMobile().removeObservers(this);
        mViewModel.getRespEnterMobile().removeObservers(this);
        mViewModel.getCountryCode().removeObservers(this);
    }
    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick: " + view.getId());
        //maneja los clicks en los elementos de la interfaz grafica
        if(view.getId()== R.id.NextBtn) {
            //Si el elemento de la interfaz grafica es el Next Button "Siguiente"
            try {
                String full_tel_number = mCountryCodePicker.getFullNumber().replace(" ","");
                //String full_tel_number = "591";
                full_tel_number = full_tel_number.replace("-","");
                full_tel_number = full_tel_number.replace("(","");
                full_tel_number = full_tel_number.replace(")","");
                String country_code = mCountryCodePicker.getSelectedCountryCode();
                //obtiene el numero de telefono ingresado
                String tel_number = full_tel_number.substring(country_code.length());

                //String tel_number = String.valueOf(mCountryCodePicker.getPhoneNumber().getNationalNumber());
                //llama la funcion para requerir autenticacion de la vista modelo
                //codigo de la unidad con su nro de telefono y codigo de pais
                mViewModel.requestVerificationCode(mCountryCodePicker.getSelectedCountryCode(), tel_number);
            }
            catch (Exception e)
            {
                Toast.makeText(this.getContext(), "Invalid phone number", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*funcion que es usada para mostrar el spinner y overlay dialog*/
    private void showSpinner() {
        mOverlayDialog.show();
        mSpinner.setVisibility(View.VISIBLE);
    }

    /*funcion que es usada para ocultar spinner y overlay dialog*/
    private void hideSpinner() {
        mOverlayDialog.dismiss();
        mSpinner.setVisibility(View.GONE);
    }

    /*Observador para el boolean live data IsWaitRespEnterMobile que indica si el proceso
    (request authentication code of the drive with his telephone number) esta corriendo*/
    private class LoadingObserver implements Observer<Boolean> {

        @Override
        public void onChanged(@Nullable Boolean isLoading) {
            if (isLoading == null) return;

            //si el proceso esta corriendo
            if (isLoading) {
                //muestra el spinner y overlay
                showSpinner();
            } else {
                //de lo contrario, oculta el spinner y overlay
                hideSpinner();
            }
        }
    }

    /*Observador para el string live data RespEnterMobile que es la respuesta del proceso
    (request authentication code of the drive with his telephone number) del servidor*/
    private class statusObserver implements Observer<String>
    {
        @Override
        public void onChanged(@Nullable String statusTxt) {
            if (statusTxt == null) return;
            //set the text in the mStatus text view with the response
            mStatus.setText(statusTxt);
        }
    }

    /*Observador para el string live data CountryCode. Es usado para mostrar el ultimo codigo de pais seleccionado
    en este fragment cuando el usiario presiona ReenviarCodigo del siguiente fragmento (ActivationCodeFragment)*/
    private class CountryCodeObserver implements Observer<String>
    {
        @Override
        public void onChanged(@Nullable String countryCode) {
            if (countryCode == null) return;
            try {
                //Establece el pais en el mCountryCodePicker country picker
                mCountryCodePicker.setCountryForPhoneCode(Integer.parseInt(countryCode));
            }
            catch (Exception e )
            {

            }
        }
    }


    /*Observador para el boolean live data IsVerificationCodeReceived que indica si that indicates si el solicitado
    código de verificación es recibido por la aplicación o no*/
    private class VerificationCodeReceivedObserver implements Observer<Boolean> {

        @Override
        public void onChanged(@Nullable Boolean isVerificationCodeReceived) {
            if (isVerificationCodeReceived == null) return;
            //si la verificacion de codigo es recivido, va al siguiente fragmento (ActivationCode)
            if (isVerificationCodeReceived) {
                mNavController.navigate(R.id.action_navigation_home_to_activation_code);
            }
        }
    }
}
