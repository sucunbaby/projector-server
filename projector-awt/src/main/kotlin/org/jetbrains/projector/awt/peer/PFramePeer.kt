/*
 * Copyright (c) 2019-2022, JetBrains s.r.o. and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. JetBrains designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact JetBrains, Na Hrebenech II 1718/10, Prague, 14000, Czech Republic
 * if you need additional information or have any questions.
 */
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package org.jetbrains.projector.awt.peer

import java.awt.Frame
import java.awt.MenuBar
import java.awt.Rectangle
import java.awt.peer.FramePeer

class PFramePeer(target: Frame) : PWindowPeer(target), FramePeer {

  override fun setTitle(title: String?) {
    pWindow.title = title
  }

  override fun setMenuBar(mb: MenuBar) {}

  override fun setResizable(resizeable: Boolean) {}

  override fun setState(state: Int) {}

  override fun getState(): Int {
    return 0
  }

  override fun setMaximizedBounds(bounds: Rectangle?) {}

  override fun setBoundsPrivate(x: Int, y: Int, width: Int, height: Int) {}

  override fun getBoundsPrivate(): Rectangle? {
    return null
  }

  override fun emulateActivation(b: Boolean) {}
}
