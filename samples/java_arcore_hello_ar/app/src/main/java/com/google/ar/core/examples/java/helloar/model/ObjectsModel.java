package com.google.ar.core.examples.java.helloar.model;

import android.graphics.Bitmap;

/**
 * Created by zunairsyed on 2017-12-13.
 */

public class ObjectsModel {

    private String name;
    private String objectFileName;
    private String diffuseTextureFileName;
    private float scaleFactor;
    private Bitmap preview;


    public ObjectsModel(String name, String objectFileName, String diffuseTextureFileName, float scaleFactor){
        this.name = name;
        this.objectFileName = objectFileName;
        this.diffuseTextureFileName = diffuseTextureFileName;
        this.scaleFactor = scaleFactor;
    }

    public String getName() {
        return name;
    }

    public ObjectsModel setName(String name) {
        this.name = name;
        return this;
    }

    public String getObjectFileName() {
        return objectFileName;
    }

    public ObjectsModel setObjectFileName(String objectFileName) {
        this.objectFileName = objectFileName;
        return this;
    }

    public String getDiffuseTextureFileName() {
        return diffuseTextureFileName;
    }

    public ObjectsModel setDiffuseTextureFileName(String diffuseTextureFileName) {
        this.diffuseTextureFileName = diffuseTextureFileName;
        return this;
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public ObjectsModel setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        return this;
    }

    public Bitmap getPreview() {
        return preview;
    }

    public ObjectsModel setPreview(Bitmap preview) {
        this.preview = preview;
        return this;
    }

    @Override
    public String toString() {
        return (name + " | " + objectFileName + " | " + scaleFactor);
    }
}
