package com.creativeapps.schoolbustracker.data.network.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;


public class Parent implements Parcelable{
    @Expose
    @SerializedName("id")
    private Integer id;

    @Expose
    @SerializedName("name")
    private String name;

    @Expose
    @SerializedName("address_latitude")
    private Double address_latitude;

    @Expose
    @SerializedName("address_longitude")
    private Double address_longitude;

    @Expose
    @SerializedName("tel_number")
    private String tel_number;


    @Expose
    @SerializedName("country_code")
    private String country_code;

    @Expose
    @SerializedName("secretKey")
    private String secretKey;

    @Expose
    @SerializedName("v_code")
    private String v_code;

    @Expose
    @SerializedName("verified")
    private Byte verified;



    @Expose
    @SerializedName("driver")
    private Driver driver;


    @Expose
    @SerializedName("school")
    private School school;

    @Expose
    @SerializedName("children")
    private List<Child> children;


    protected Parent(Parcel in) {
        id = in.readInt();
        name = in.readString();
        address_latitude = in.readDouble();
        address_longitude = in.readDouble();
        tel_number = in.readString();
        country_code = in.readString();
        secretKey = in.readString();
        v_code = in.readString();
        verified = in.readByte();
        driver = in.readParcelable(Driver.class.getClassLoader());
        school = in.readParcelable(School.class.getClassLoader());
        children = in.readParcelable(Child.class.getClassLoader());
    }

    public static final Creator<Parent> CREATOR = new Creator<Parent>() {
        @Override
        public Parent createFromParcel(Parcel in) {

            return new Parent(in);
        }

        @Override
        public Parent[] newArray(int size) {
            return new Parent[size];
        }
    };

    public Parent() {

    }

    public Parent(Integer id, String name, Double address_latitude, Double address_longitude,
                  String tel_number, String country_code, String secretKey, String v_code, Byte verified) {
        this.id = id;
        this.name = name;
        this.address_latitude = address_latitude;
        this.address_longitude = address_longitude;
        this.tel_number = tel_number;
        this.country_code = country_code;
        this.secretKey = secretKey;
        this.v_code = v_code;
        this.verified = verified;
    }


    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getAddress_latitude() {
        return address_latitude;
    }

    public Double getAddress_longitude() {
        return address_longitude;
    }

    public String getTel_number() {
        return tel_number;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getV_code() {
        return v_code;
    }

    public Byte getVerified() {
        return verified;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress_latitude(Double address_latitude) {
        this.address_latitude = address_latitude;
    }

    public void setAddress_longitude(Double address_longitude) {
        this.address_longitude = address_longitude;
    }

    public void setTel_number(String tel_number) {
        this.tel_number = tel_number;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setV_code(String v_code) {
        this.v_code = v_code;
    }

    public void setVerified(Byte verified) {
        this.verified = verified;
    }

    public String getCountry_code() {
        return country_code;
    }

    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }


    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(name);
        parcel.writeDouble(address_latitude);
        parcel.writeDouble(address_longitude);
        parcel.writeString(tel_number);
        parcel.writeString(country_code);
        parcel.writeString(secretKey);
        parcel.writeString(v_code);
        parcel.writeByte(verified);
    }

    public List<Child> getChildren() {
        return children;
    }

    public void setChildren(List<Child> children) {
        this.children = children;
    }

}
