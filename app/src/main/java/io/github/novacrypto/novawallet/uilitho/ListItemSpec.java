/*
 *  NovaWallet, Cryptocurrency Wallet for Android
 *  Copyright (C) 2018 Alan Evans, NovaCrypto
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *  Original source: https://github.com/NovaCrypto/NovaWallet
 *  You can contact the authors via github issues.
 */

package io.github.novacrypto.novawallet.uilitho;

import android.support.annotation.DrawableRes;

import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Image;
import com.facebook.litho.widget.Text;

import static com.facebook.yoga.YogaAlign.CENTER;
import static com.facebook.yoga.YogaEdge.ALL;

@LayoutSpec
public class ListItemSpec {

    @OnCreateLayout
    static ComponentLayout onCreateLayout(
            ComponentContext c,
            @Prop int color,
            @Prop @DrawableRes int icon,
            @Prop String title,
            @Prop String subtitle,
            @Prop String balance
    ) {

        return Column.create(c)
                .paddingDip(ALL, 16)
                .backgroundColor(color)
                .child(Row.create(c)
                        .alignItems(CENTER)
                        .child(Image.create(c)
                                .drawableRes(icon)
                                .widthDip(20)
                                .heightDip(20)
                                .build())
                        .child(Text.create(c)
                                .text(title)
                                .textSizeSp(30)))
                .child(Text.create(c)
                        .text(subtitle)
                        .textSizeSp(14))
                .child(Text.create(c)
                        .text(balance)
                        .textSizeSp(14))
                .build();
    }
}