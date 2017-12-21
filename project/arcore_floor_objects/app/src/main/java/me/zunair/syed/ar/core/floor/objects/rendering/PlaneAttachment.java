

package me.zunair.syed.ar.core.floor.objects.rendering;

import me.zunair.syed.ar.core.floor.objects.model.ObjectsModel;
import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;

/**
 * This class tracks the attachment of object's Anchor to a Plane. It will construct a pose
 * that will stay on the plane (in Y direction), while still properly tracking the XZ changes
 * from the anchor updates.
 */
public class PlaneAttachment {
    private final Plane mPlane;
    private final Anchor mAnchor;

    private float mScaleFactor;
    private ObjectsModel mModel;

    // Allocate temporary storage to avoid multiple allocations per frame.
    private final float[] mPoseTranslation = new float[3];
    private final float[] mPoseRotation = new float[4];

    public PlaneAttachment(Plane plane, Anchor anchor, ObjectsModel model, float scaleFactor) {
        mPlane = plane;
        mAnchor = anchor;
        mScaleFactor = scaleFactor;
        mModel = model;
    }

    public boolean isTracking() {
        return /*true if*/
            mPlane.getTrackingState() == Plane.TrackingState.TRACKING &&
            mAnchor.getTrackingState() == Anchor.TrackingState.TRACKING;
    }

    public Pose getPose() {
        Pose pose = mAnchor.getPose();
        pose.getTranslation(mPoseTranslation, 0);
        pose.getRotationQuaternion(mPoseRotation, 0);
        mPoseTranslation[1] = mPlane.getCenterPose().ty();
        return new Pose(mPoseTranslation, mPoseRotation);
    }

    public Anchor getAnchor() {
        return mAnchor;
    }

    public ObjectsModel getModel() { return mModel; }
    public float getScaleFactor() { return mScaleFactor; }

    public void setScaleFactor(float scaleFactor){
        this.mScaleFactor = scaleFactor;
    }
}
