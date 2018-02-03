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

package io.github.novacrypto.security;

public final class Base16 {

    public String encode(final byte[] array) {
        final StringBuilder sb = new StringBuilder(array.length * 2);
        for (final byte b : array) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public byte[] decode(final String data) {
        final int length = data.length();
        if (length % 2 == 1) throw new RuntimeException("Odd length unexpected");
        final byte[] bytes = new byte[length / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (parseHex(data.charAt(i * 2)) << 4 | parseHex(data.charAt(i * 2 + 1)));
        }
        return bytes;
    }

    private static int parseHex(final char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return (c - 'a') + 10;
        if (c >= 'A' && c <= 'F') return (c - 'A') + 10;
        throw new RuntimeException("Invalid hex char '" + c + '\'');
    }
}
