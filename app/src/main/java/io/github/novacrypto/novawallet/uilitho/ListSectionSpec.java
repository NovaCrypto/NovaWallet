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

import android.graphics.Color;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.SingleComponentSection;

import java.util.List;

import io.github.novacrypto.novawallet.AddressModel;

@GroupSectionSpec
public class ListSectionSpec {

    @OnCreateChildren
    static Children onCreateChildren(final SectionContext c, @Prop List<AddressModel> data) {
        Children.Builder builder = Children.create();

        final int size = data.size();
        for (int i = 0; i < size; i++) {
            final AddressModel addressModel = data.get(i);
            builder.child(SingleComponentSection.create(c)
                                                .key(String.valueOf(i))
                                                .component(ListItem.create(c)
                                                                   .color(i % 2 == 0 ? Color.WHITE : Color.LTGRAY)
                                                                   .icon(addressModel.getCoinIcon())
                                                                   .title(addressModel.getAddress())
                                                                   .subtitle(addressModel.getValue())
                                                                   .build()));
        }
        return builder.build();
    }
}