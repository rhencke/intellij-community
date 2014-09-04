/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.python.hierarchy.call;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import com.jetbrains.python.hierarchy.PyHierarchyNodeDescriptor;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyElement;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFunction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author novokrest
 */
public class PyCallerFunctionTreeStructure extends HierarchyTreeStructure {
  private final String myScopeType;

  public PyCallerFunctionTreeStructure(Project project, PsiElement element, String currentScopeType) {
    super(project, new PyHierarchyNodeDescriptor(null, element, true));
    myScopeType = currentScopeType;
  }

  @NotNull
  @Override
  protected Object[] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
    final List<PyHierarchyNodeDescriptor> descriptors = new ArrayList<PyHierarchyNodeDescriptor>();
    if (descriptor instanceof PyHierarchyNodeDescriptor) {
      final PyHierarchyNodeDescriptor pyDescriptor = (PyHierarchyNodeDescriptor)descriptor;
      final PsiElement element = pyDescriptor.getPsiElement();
      final boolean isCallable = element instanceof PyFunction || element instanceof PyClass || element instanceof PyFile;
      HierarchyNodeDescriptor nodeDescriptor = getBaseDescriptor();
      if (!(element instanceof PyElement) || !isCallable || nodeDescriptor == null) {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
      }

      final List<PsiElement> callers = new ArrayList<PsiElement>();

      PyCallDataManager[] functionManagers = {
        // TODO: Add dynamic call data manager
        PyStaticCallDataManager.getInstance(myProject),
      };

      for (PyCallDataManager functionManager : functionManagers) {
        callers.addAll(functionManager.getCallers((PyElement)element));
      }

      final HashMap<PsiElement, PyHierarchyNodeDescriptor> callerToDescriptorMap = new HashMap<PsiElement, PyHierarchyNodeDescriptor>();
      PsiElement baseClass = element instanceof PyFunction ? ((PyFunction)element).getContainingClass() : null;

      for (PsiElement caller : callers) {
        if (baseClass != null && !isInScope(baseClass, caller, myScopeType)) continue;

        PyHierarchyNodeDescriptor callerDescriptor = callerToDescriptorMap.get(caller);
        if (callerDescriptor == null) {
          callerDescriptor = new PyHierarchyNodeDescriptor(descriptor, caller, false);
          callerToDescriptorMap.put(caller, callerDescriptor);
          descriptors.add(callerDescriptor);
        }
      }

    }
    return ArrayUtil.toObjectArray(descriptors);
  }

  @Override
  public boolean isAlwaysShowPlus() {
    return true;
  }
}
