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

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class TextViewCompatImplICS extends TextViewCompatImplBase {
	@Override
	public boolean supportsSetAllCaps() {
		return true;
	}

	@Override
	public void setAllCaps(TextView textView, boolean allCaps) {
		textView.setAllCaps(allCaps);
	}
}