package ml.docilealligator.infinityforreddit.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.core.view.MenuItemCompat;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigationrail.NavigationRailView;
import java.util.Objects;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;

public class NavigationWrapper {
    public BottomAppBar bottomAppBar;
    public LinearLayout linearLayoutBottomAppBar;
    public SignalNavigationItemView option1BottomAppBar;
    public SignalNavigationItemView option2BottomAppBar;
    public SignalNavigationItemView option3BottomAppBar;
    public SignalNavigationItemView option4BottomAppBar;

    public NavigationRailView navigationRailView;
    public FloatingActionButton floatingActionButton;

    private CustomThemeWrapper customThemeWrapper;
    private int option1 = -1;
    private int option2 = -1;
    private int option3 = -1;
    private int option4 = -1;

    private int inboxCount;
    @Nullable
    private BadgeDrawable badgeDrawable;
    @Nullable
    private View badgedView;
    @Nullable
    private MenuItem badgedMenuItem;
    @Nullable
    private View.OnLayoutChangeListener badgeLayoutListener;

    public NavigationWrapper(BottomAppBar bottomAppBar, LinearLayout linearLayoutBottomAppBar,
                             SignalNavigationItemView option1BottomAppBar, SignalNavigationItemView option2BottomAppBar,
                             SignalNavigationItemView option3BottomAppBar, SignalNavigationItemView option4BottomAppBar,
                             FloatingActionButton floatingActionButton, NavigationRailView navigationRailView,
                             CustomThemeWrapper customThemeWrapper,
                             boolean showBottomAppBar) {
        this.bottomAppBar = bottomAppBar;
        this.linearLayoutBottomAppBar = linearLayoutBottomAppBar;
        this.option1BottomAppBar = option1BottomAppBar;
        this.option2BottomAppBar = option2BottomAppBar;
        this.option3BottomAppBar = option3BottomAppBar;
        this.option4BottomAppBar = option4BottomAppBar;
        this.navigationRailView = navigationRailView;
        this.customThemeWrapper = customThemeWrapper;
        if (navigationRailView != null) {
            if (showBottomAppBar) {
                this.floatingActionButton = (FloatingActionButton) Objects.requireNonNull(navigationRailView.getHeaderView());
            } else {
                navigationRailView.setVisibility(View.GONE);
                this.floatingActionButton = floatingActionButton;
            }
        } else {
            this.floatingActionButton = floatingActionButton;
        }
    }

    public void applyCustomTheme(int bottomAppBarIconColor, int bottomAppBarBackgroundColor) {
        if (navigationRailView == null) {
            option1BottomAppBar.setIconTint(bottomAppBarIconColor);
            option2BottomAppBar.setIconTint(bottomAppBarIconColor);
            option3BottomAppBar.setIconTint(bottomAppBarIconColor);
            option4BottomAppBar.setIconTint(bottomAppBarIconColor);
            bottomAppBar.setBackgroundTint(ColorStateList.valueOf(bottomAppBarBackgroundColor));
        } else {
            navigationRailView.setBackgroundColor(bottomAppBarBackgroundColor);
            applyMenuItemTheme(navigationRailView.getMenu(), bottomAppBarIconColor);
        }
    }

    @SuppressLint("RestrictedApi")
    private void applyMenuItemTheme(Menu menu, int bottomAppBarIconColor) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (((MenuItemImpl) item).requestsActionButton()) {
                MenuItemCompat.setIconTintList(item, ColorStateList.valueOf(bottomAppBarIconColor));
            }
        }
    }

    public void bindOptionDrawableResource(int... imageResources) {
        if (navigationRailView == null) {
            bottomAppBar.setVisibility(View.VISIBLE);
        } else {
            navigationRailView.setVisibility(View.VISIBLE);
        }

        if (imageResources.length == 2) {
            if (navigationRailView == null) {
                linearLayoutBottomAppBar.setWeightSum(2);
                option1BottomAppBar.setVisibility(View.GONE);
                option3BottomAppBar.setVisibility(View.GONE);

                option2BottomAppBar.setImageResource(imageResources[0]);
                option4BottomAppBar.setImageResource(imageResources[1]);
            } else {
                Menu menu = navigationRailView.getMenu();
                menu.findItem(R.id.navigation_rail_option_1).setIcon(imageResources[0]);
                menu.findItem(R.id.navigation_rail_option_2).setIcon(imageResources[1]);
                menu.findItem(R.id.navigation_rail_option_3).setVisible(false);
                menu.findItem(R.id.navigation_rail_option_4).setVisible(false);
            }
        } else {
            if (navigationRailView == null) {
                // Undo what the two-option layout hides, so rebinding back to four options restores
                // the full bar instead of leaving two icons gone and the weights short.
                linearLayoutBottomAppBar.setWeightSum(4);
                option1BottomAppBar.setVisibility(View.VISIBLE);
                option3BottomAppBar.setVisibility(View.VISIBLE);

                option1BottomAppBar.setImageResource(imageResources[0]);
                option2BottomAppBar.setImageResource(imageResources[1]);
                option3BottomAppBar.setImageResource(imageResources[2]);
                option4BottomAppBar.setImageResource(imageResources[3]);
            } else {
                Menu menu = navigationRailView.getMenu();
                menu.findItem(R.id.navigation_rail_option_1).setIcon(imageResources[0]);
                menu.findItem(R.id.navigation_rail_option_2).setIcon(imageResources[1]);
                menu.findItem(R.id.navigation_rail_option_3).setIcon(imageResources[2]);
                menu.findItem(R.id.navigation_rail_option_4).setIcon(imageResources[3]);
                menu.findItem(R.id.navigation_rail_option_3).setVisible(true);
                menu.findItem(R.id.navigation_rail_option_4).setVisible(true);
            }
        }
    }

    public void bindOptions(int... options) {
        // Clear every slot first: rebinding to a smaller bar must not leave the previous layout's
        // options behind in the slots it no longer fills.
        option1 = -1;
        option2 = -1;
        option3 = -1;
        option4 = -1;
        if (options.length == 2) {
            if (navigationRailView == null) {
                option2 = options[0];
                option4 = options[1];
            } else {
                option1 = options[0];
                option2 = options[1];
            }
        } else {
            option1 = options[0];
            option2 = options[1];
            option3 = options[2];
            option4 = options[3];
        }
    }

    public void setOtherActivitiesContentDescription(Context context, View view, int option) {
        setItemLabelAndContentDescription(view, getOtherActivitiesContentDescription(context, option));
    }

    public void setItemLabelAndContentDescription(View view, CharSequence label) {
        view.setContentDescription(label);
        if (view instanceof SignalNavigationItemView signalNavigationItemView) {
            signalNavigationItemView.setLabel(label);
        }
    }

    public void setRailItemTitles(Context context, int... options) {
        String[] titles = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            titles[i] = getOtherActivitiesContentDescription(context, options[i]);
        }
        setRailItemTitles(titles);
    }

    public void setRailItemTitles(String... titles) {
        if (navigationRailView == null) {
            return;
        }
        Menu menu = navigationRailView.getMenu();
        int[] itemIds = {R.id.navigation_rail_option_1, R.id.navigation_rail_option_2,
                R.id.navigation_rail_option_3, R.id.navigation_rail_option_4};
        for (int i = 0; i < titles.length && i < itemIds.length; i++) {
            menu.findItem(itemIds[i]).setTitle(titles[i]);
        }
    }

    public void setActiveItem(int position) {
        if (navigationRailView == null) {
            option1BottomAppBar.setActive(position == 1);
            option2BottomAppBar.setActive(position == 2);
            option3BottomAppBar.setActive(position == 3);
            option4BottomAppBar.setActive(position == 4);
            return;
        }
        Menu menu = navigationRailView.getMenu();
        int[] itemIds = {R.id.navigation_rail_option_1, R.id.navigation_rail_option_2,
                R.id.navigation_rail_option_3, R.id.navigation_rail_option_4};
        for (int i = 0; i < itemIds.length; i++) {
            menu.findItem(itemIds[i]).setChecked(i + 1 == position);
        }
    }

    public static String getOtherActivitiesContentDescription(Context context, int option) {
        switch (option) {
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_HOME:
                return context.getString(R.string.content_description_home);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_SUBSCRIPTIONS:
                return context.getString(R.string.content_description_subscriptions);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_INBOX:
                return context.getString(R.string.content_description_inbox);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_PROFILE:
                return context.getString(R.string.content_description_profile);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_MULTIREDDITS:
                return context.getString(R.string.content_description_multireddits);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_SUBMIT_POSTS:
                return context.getString(R.string.content_description_submit_post);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_REFRESH:
                return context.getString(R.string.content_description_refresh);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_CHANGE_SORT_TYPE:
                return context.getString(R.string.content_description_change_sort_type);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_CHANGE_POST_LAYOUT:
                return context.getString(R.string.content_description_change_post_layout);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_SEARCH:
                return context.getString(R.string.content_description_search);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_GO_TO_SUBREDDIT:
                return context.getString(R.string.content_description_go_to_subreddit);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_GO_TO_USER:
                return context.getString(R.string.content_description_go_to_user);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_HIDE_READ_POSTS:
                return context.getString(R.string.content_description_hide_read_posts);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_FILTER_POSTS:
                return context.getString(R.string.content_description_filter_posts);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_UPVOTED:
                return context.getString(R.string.content_description_upvoted);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_DOWNVOTED:
                return context.getString(R.string.content_description_downvoted);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_HIDDEN:
                return context.getString(R.string.content_description_hidden);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_SAVED:
                return context.getString(R.string.content_description_saved);
            case SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_GO_TO_TOP:
            default:
                return context.getString(R.string.content_description_go_to_top);
        }
    }

    public void showNavigation() {
        if (bottomAppBar != null) {
            bottomAppBar.performShow();
        }
    }

    public void hideNavigation() {
        if (bottomAppBar != null) {
            bottomAppBar.performHide();
        }
    }

    public void showFab() {
        if (navigationRailView == null) {
            floatingActionButton.show();
        }
    }

    public void hideFab() {
        if (navigationRailView == null) {
            floatingActionButton.hide();
        }
    }

    @ExperimentalBadgeUtils
    public void setInboxCount(Context context, int inboxCount) {
        this.inboxCount = Math.max(0, inboxCount);
        detachBadge();

        if (this.inboxCount == 0) {
            return;
        }

        if (navigationRailView != null) {
            Menu menu = navigationRailView.getMenu();
            int[] itemIds = {R.id.navigation_rail_option_1, R.id.navigation_rail_option_2,
                    R.id.navigation_rail_option_3, R.id.navigation_rail_option_4};
            int[] boundOptions = {option1, option2, option3, option4};
            for (int i = 0; i < itemIds.length; i++) {
                if (isInboxOption(boundOptions[i])) {
                    badgedMenuItem = menu.findItem(itemIds[i]);
                    badgeDrawable = BadgeDrawable.create(context);
                    badgeDrawable.setNumber(inboxCount);
                    badgeDrawable.setBackgroundColor(customThemeWrapper.getColorAccent());
                    badgeDrawable.setBadgeTextColor(customThemeWrapper.getButtonTextColor());
                    BadgeUtils.attachBadgeDrawable(badgeDrawable, badgedMenuItem);
                    return;
                }
            }
            return;
        }

        SignalNavigationItemView anchorView = getInboxOptionView();
        if (anchorView == null) {
            return;
        }

        badgedView = anchorView;
        // The badge is offset by half the icon width, so it has to be positioned from the anchor's
        // laid-out width and repositioned whenever that width changes: it is zero before the first
        // layout, and it changes again when the bar is rebound to a different number of options.
        badgeLayoutListener = new View.OnLayoutChangeListener() {
            @ExperimentalBadgeUtils
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right - left != oldRight - oldLeft) {
                    applyBadge(context);
                }
            }
        };
        anchorView.addOnLayoutChangeListener(badgeLayoutListener);
        applyBadge(context);
    }

    /** Draws the badge on the current anchor, once that anchor has a width to position it against. */
    @ExperimentalBadgeUtils
    private void applyBadge(Context context) {
        View anchorView = badgedView;
        if (anchorView == null || inboxCount == 0 || anchorView.getWidth() == 0) {
            return;
        }

        if (badgeDrawable != null) {
            BadgeUtils.detachBadgeDrawable(badgeDrawable, anchorView);
        }
        badgeDrawable = createBadgeDrawable(context, inboxCount, anchorView);
        BadgeUtils.attachBadgeDrawable(badgeDrawable, anchorView);
    }

    @ExperimentalBadgeUtils
    private void detachBadge() {
        if (badgedView != null && badgeDrawable != null) {
            BadgeUtils.detachBadgeDrawable(badgeDrawable, badgedView);
        }
        if (badgedMenuItem != null && badgeDrawable != null) {
            BadgeUtils.detachBadgeDrawable(badgeDrawable, badgedMenuItem);
        }
        if (badgedView != null && badgeLayoutListener != null) {
            badgedView.removeOnLayoutChangeListener(badgeLayoutListener);
        }
        badgeDrawable = null;
        badgeLayoutListener = null;
        badgedView = null;
        badgedMenuItem = null;
    }

    @Nullable
    private SignalNavigationItemView getInboxOptionView() {
        if (isInboxOption(option1)) {
            return option1BottomAppBar;
        } else if (isInboxOption(option2)) {
            return option2BottomAppBar;
        } else if (isInboxOption(option3)) {
            return option3BottomAppBar;
        } else if (isInboxOption(option4)) {
            return option4BottomAppBar;
        }
        return null;
    }

    private static boolean isInboxOption(int option) {
        return option == SharedPreferencesUtils.MAIN_ACTIVITY_BOTTOM_APP_BAR_OPTION_INBOX
                || option == SharedPreferencesUtils.OTHER_ACTIVITIES_BOTTOM_APP_BAR_OPTION_INBOX;
    }

    private BadgeDrawable createBadgeDrawable(Context context, int inboxCount, View anchorView) {
        BadgeDrawable badgeDrawable = BadgeDrawable.create(context);
        badgeDrawable.setNumber(inboxCount);
        badgeDrawable.setBackgroundColor(customThemeWrapper.getColorAccent());
        badgeDrawable.setBadgeTextColor(customThemeWrapper.getButtonTextColor());
        badgeDrawable.setHorizontalOffset(anchorView.getWidth() / 2);

        return badgeDrawable;
    }
}
