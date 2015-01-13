/*
 * Copyright (C) 2015 Haruki Hasegawa <h6a.h4i.0@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.astuetz;

import android.os.Build;
import android.view.ViewTreeObserver;

class ViewTreeObserverCompat {
	private static final ViewTreeObserverCompatImplBase IMPL;

	static {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			IMPL = new ViewTreeObserverCompatImplJB();
		} else {
			IMPL = new ViewTreeObserverCompatImplBase();
		}
	}

	public static void removeOnGlobalLayoutListener(ViewTreeObserver observer, ViewTreeObserver.OnGlobalLayoutListener victim) {
		IMPL.removeOnGlobalLayoutListener(observer, victim);
	}
}