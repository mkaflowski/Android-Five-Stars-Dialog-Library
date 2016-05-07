package angtrim.com.fivestarslibrary;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;


public class FiveStarsDialog implements DialogInterface.OnClickListener {

    private static String DEFAULT_TITLE = "Rate this app";
    private static String DEFAULT_TEXT = "How much do you like our app?";
    private static String DEFAULT_POSITIVE = "Ok";
    private static String DEFAULT_NEGATIVE = "Later";
    private static String DEFAULT_NEVER = "Never";

    private static final String SP_NUM_OF_ACCESS = "numOfAccess";
    private static final String SP_DISABLED = "disabled";

    private static String TAG = FiveStarsDialog.class.getSimpleName();
    private final Context context;
    private boolean isForceMode = false;
    SharedPreferences sharedPrefs;
    private String supportEmail;
    private RatingBar ratingBar;

    private String title = null;
    private String rateText = null;
    private String positive = null;
    private String negative = null;
    private String never = null;

    private int style = 0;

    private AlertDialog alertDialog;
    private int upperBound = 4;
    private NegativeReviewListener negativeReviewListener;
    private ReviewListener reviewListener;
    private boolean showOnZeroStars = false;

    //test


    public FiveStarsDialog(Context context, String supportEmail) {
        this.context = context;
        sharedPrefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        this.supportEmail = supportEmail;

    }

    private void build() {
        AlertDialog.Builder builder;

        if (style != 0) {
            ContextThemeWrapper ctw = new ContextThemeWrapper(context, style);
            builder = new AlertDialog.Builder(ctw);
        } else
            builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.stars, null);

        String titleToAdd = (title == null) ? DEFAULT_TITLE : title;
        String textToAdd = (rateText == null) ? DEFAULT_TEXT : rateText;
        String positiveToAdd = (positive == null) ? DEFAULT_POSITIVE : positive;
        String negativeToAdd = (negative == null) ? DEFAULT_NEGATIVE : negative;
        String neverToAdd = (negative == null) ? DEFAULT_NEVER : never;

        TextView contentTextView = (TextView) dialogView.findViewById(R.id.text_content);
        contentTextView.setText(textToAdd);
        ratingBar = (RatingBar) dialogView.findViewById(R.id.ratingBar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                Log.d(TAG, "Rating changed : " + v);
                if (isForceMode && (v >= upperBound || v == 0)) {
                    openMarket();
                    if (reviewListener != null)
                        reviewListener.onReview((int) ratingBar.getRating());
                }
            }
        });
        alertDialog = builder.setTitle(titleToAdd)
                .setView(dialogView)
                .setNegativeButton(negativeToAdd, this)
                .setPositiveButton(positiveToAdd, this)
                .setNeutralButton(neverToAdd, this)
                .create();
    }

    /**
     * set languages from xml resources
     */
    public FiveStarsDialog setInternational() {
        DEFAULT_TITLE = context.getString(R.string.title_rate);
        DEFAULT_TEXT = context.getString(R.string.text);
        DEFAULT_POSITIVE = context.getString(R.string.ok);
        DEFAULT_NEGATIVE = context.getString(R.string.not_now);
        DEFAULT_NEVER = context.getString(R.string.never);

        return this;
    }


    private void disable() {
        SharedPreferences shared = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putBoolean(SP_DISABLED, true);
        editor.apply();
    }

    private void openMarket() {
        final String appPackageName = context.getPackageName();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }


    private void sendEmail() {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{supportEmail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "App Report (" + context.getPackageName() + ")");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }


    private void show() {
        boolean disabled = sharedPrefs.getBoolean(SP_DISABLED, false);
        if (!disabled) {
            build();
            alertDialog.show();
        }
    }

    public void forceShow() {
            build();
            alertDialog.show();
    }

    public void showAfter(int numberOfAccess) {
        build();
        SharedPreferences.Editor editor = sharedPrefs.edit();
        int numOfAccess = sharedPrefs.getInt(SP_NUM_OF_ACCESS, 0);
        editor.putInt(SP_NUM_OF_ACCESS, numOfAccess + 1);
        editor.apply();
        if (numOfAccess + 1 >= numberOfAccess) {
            show();
        }
    }


    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == DialogInterface.BUTTON_POSITIVE) {
            if (ratingBar.getRating() < upperBound && ratingBar.getRating() > 0 || (ratingBar.getRating() == 0 && !showOnZeroStars)) {
                if (negativeReviewListener == null) {
                    sendEmail();
                } else {
                    negativeReviewListener.onNegativeReview((int) ratingBar.getRating());
                }

            } else
                openMarket();

            disable();
            if (reviewListener != null)
                reviewListener.onReview((int) ratingBar.getRating());
        }
        if (i == DialogInterface.BUTTON_NEUTRAL) {
            disable();
        }
        if (i == DialogInterface.BUTTON_NEGATIVE) {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putInt(SP_NUM_OF_ACCESS, 0);
            editor.apply();
        }
        alertDialog.hide();
    }

    /**
     * @param styleR i.e. R.style.LE_THEME
     */
    public FiveStarsDialog setStyle(int styleR) {
        style = styleR;
        return this;
    }

    public FiveStarsDialog setShowOnZeroStars(boolean showOnZeroStars) {
        this.showOnZeroStars = showOnZeroStars;
        return this;
    }

    public FiveStarsDialog setTitle(String title) {
        this.title = title;
        return this;

    }

    public FiveStarsDialog setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
        return this;
    }

    public FiveStarsDialog setRateText(String rateText) {
        this.rateText = rateText;
        return this;
    }

    public FiveStarsDialog setPositiveText(String text) {
        this.positive = text;
        return this;
    }

    public FiveStarsDialog setNegativeText(String text) {
        this.negative = text;
        return this;
    }

    public FiveStarsDialog setNeverText(String text) {
        this.never = text;
        return this;
    }

    /**
     * Set to true if you want to send the user directly to the market
     *
     * @param isForceMode
     * @return
     */
    public FiveStarsDialog setForceMode(boolean isForceMode) {
        this.isForceMode = isForceMode;
        return this;
    }

    /**
     * Set the upper bound for the rating.
     * If the rating is >= of the bound, the market is opened.
     *
     * @param bound the upper bound
     * @return the dialog
     */
    public FiveStarsDialog setUpperBound(int bound) {
        this.upperBound = bound;
        return this;
    }

    /**
     * Set a custom listener if you want to OVERRIDE the default "send email" action when the user gives a negative review
     *
     * @param listener
     * @return
     */
    public FiveStarsDialog setNegativeReviewListener(NegativeReviewListener listener) {
        this.negativeReviewListener = listener;
        return this;
    }

    /**
     * Set a listener to get notified when a review (positive or negative) is issued, for example for tracking purposes
     *
     * @param listener
     * @return
     */
    public FiveStarsDialog setReviewListener(ReviewListener listener) {
        this.reviewListener = listener;
        return this;
    }

}
