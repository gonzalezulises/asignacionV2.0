error id: jar:file://<HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/apache/arrow/arrow-vector/2.0.0/arrow-vector-2.0.0-sources.jar!/codegen/templates/DenseUnionVector.java
file://<WORKSPACE>/jar:file:<HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/apache/arrow/arrow-vector/2.0.0/arrow-vector-2.0.0-sources.jar!/codegen/templates/DenseUnionVector.java
### java.lang.Exception: Unexpected symbol '#' at word pos: '35' Line: '  <#list  vv.types as type>'

Java indexer failed with and exception.
```Java
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.arrow.memory.ArrowBuf;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.ReferenceManager;
import org.apache.arrow.memory.util.CommonUtil;
import org.apache.arrow.util.DataSizeRoundingUtil;
import org.apache.arrow.util.Preconditions;
import org.apache.arrow.vector.BaseValueVector;
import org.apache.arrow.vector.BitVectorHelper;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.ValueVector;
import org.apache.arrow.vector.complex.AbstractStructVector;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.complex.NonNullableStructVector;
import org.apache.arrow.vector.complex.StructVector;
import org.apache.arrow.vector.compare.VectorVisitor;
import org.apache.arrow.vector.types.Types;
import org.apache.arrow.vector.types.UnionMode;
import org.apache.arrow.vector.compare.RangeEqualsVisitor;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.util.CallBack;
import org.apache.arrow.vector.util.TransferPair;

import java.util.Arrays;
import java.util.stream.Collectors;

<@pp.dropOutputFile />
<@pp.changeOutputFile name="/org/apache/arrow/vector/complex/DenseUnionVector.java" />


<#include "/@includes/license.ftl" />

package org.apache.arrow.vector.complex;

<#include "/@includes/vv_imports.ftl" />
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import org.apache.arrow.memory.util.CommonUtil;
import org.apache.arrow.memory.util.hash.ArrowBufHasher;
import org.apache.arrow.memory.util.hash.SimpleHasher;
import org.apache.arrow.vector.compare.VectorVisitor;
import org.apache.arrow.vector.complex.impl.ComplexCopier;
import org.apache.arrow.vector.util.CallBack;
import org.apache.arrow.vector.ipc.message.ArrowFieldNode;
import org.apache.arrow.vector.BaseValueVector;
import org.apache.arrow.vector.util.OversizedAllocationException;
import org.apache.arrow.util.DataSizeRoundingUtil;
import org.apache.arrow.util.Preconditions;

import static org.apache.arrow.vector.types.UnionMode.Dense;



/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")


/**
 * A vector which can hold values of different types. It does so by using a StructVector which contains a vector for each
 * primitive type that is stored. StructVector is used in order to take advantage of its serialization/deserialization methods,
 * as well as the addOrGet method.
 *
 * For performance reasons, DenseUnionVector stores a cached reference to each subtype vector, to avoid having to do the struct lookup
 * each time the vector is accessed.
 * Source code generated using FreeMarker template ${.template_name}
 */
public class DenseUnionVector implements FieldVector {

  private String name;
  private BufferAllocator allocator;
  int valueCount;

  NonNullableStructVector internalStruct;
  private ArrowBuf typeBuffer;
  private ArrowBuf offsetBuffer;

  /**
   * The key is type Id, and the value is vector.
   */
  private ValueVector[] childVectors = new ValueVector[Byte.MAX_VALUE + 1];

  /**
   * The index is the type id, and the value is the type field.
   */
  private Field[] typeFields = new Field[Byte.MAX_VALUE + 1];
  /**
   * The index is the index into the typeFields array, and the value is the logical field id.
   */
  private byte[] typeMapFields = new byte[Byte.MAX_VALUE + 1];

  /**
   * The next typd id to allocate.
   */
  private byte nextTypeId = 0;

  private FieldReader reader;

  private final CallBack callBack;
  private long typeBufferAllocationSizeInBytes;
  private long offsetBufferAllocationSizeInBytes;

  private final FieldType fieldType;

  public static final byte TYPE_WIDTH = 1;
  public static final byte OFFSET_WIDTH = 4;

  private static final FieldType INTERNAL_STRUCT_TYPE = new FieldType(/*nullable*/ false,
          ArrowType.Struct.INSTANCE, /*dictionary*/ null, /*metadata*/ null);

  public static DenseUnionVector empty(String name, BufferAllocator allocator) {
    FieldType fieldType = FieldType.nullable(new ArrowType.Union(
            UnionMode.Dense, null));
    return new DenseUnionVector(name, allocator, fieldType, null);
  }

  public DenseUnionVector(String name, BufferAllocator allocator, FieldType fieldType, CallBack callBack) {
    this.name = name;
    this.allocator = allocator;
    this.fieldType = fieldType;
    this.internalStruct = new NonNullableStructVector(
        "internal",
        allocator,
        INTERNAL_STRUCT_TYPE,
        callBack,
        AbstractStructVector.ConflictPolicy.CONFLICT_REPLACE,
        false);
    this.typeBuffer = allocator.getEmpty();
    this.callBack = callBack;
    this.typeBufferAllocationSizeInBytes = BaseValueVector.INITIAL_VALUE_ALLOCATION * TYPE_WIDTH;
    this.offsetBuffer = allocator.getEmpty();
    this.offsetBufferAllocationSizeInBytes = BaseValueVector.INITIAL_VALUE_ALLOCATION * OFFSET_WIDTH;
  }

  public BufferAllocator getAllocator() {
    return allocator;
  }

  @Override
  public MinorType getMinorType() {
    return MinorType.DENSEUNION;
  }

  @Override
  public void initializeChildrenFromFields(List<Field> children) {
    for (Field field : children) {
      byte typeId = registerNewTypeId(field);
      FieldVector vector = (FieldVector) internalStruct.add(field.getName(), field.getFieldType());
      vector.initializeChildrenFromFields(field.getChildren());
      childVectors[typeId] = vector;
    }
  }

  @Override
  public List<FieldVector> getChildrenFromFields() {
    return internalStruct.getChildrenFromFields();
  }

  @Override
  public void loadFieldBuffers(ArrowFieldNode fieldNode, List<ArrowBuf> ownBuffers) {
    if (ownBuffers.size() != 2) {
      throw new IllegalArgumentException("Illegal buffer count for dense union with type " + getField().getFieldType() +
          ", expected " + 2 + ", got: " + ownBuffers.size());
    }

    ArrowBuf buffer = ownBuffers.get(0);
    typeBuffer.getReferenceManager().release();
    typeBuffer = buffer.getReferenceManager().retain(buffer, allocator);
    typeBufferAllocationSizeInBytes = typeBuffer.capacity();

    buffer = ownBuffers.get(1);
    offsetBuffer.getReferenceManager().release();
    offsetBuffer = buffer.getReferenceManager().retain(buffer, allocator);
    offsetBufferAllocationSizeInBytes = offsetBuffer.capacity();

    this.valueCount = fieldNode.getLength();
  }

  @Override
  public List<ArrowBuf> getFieldBuffers() {
    List<ArrowBuf> result = new ArrayList<>(2);
    setReaderAndWriterIndex();
    result.add(typeBuffer);
    result.add(offsetBuffer);

    return result;
  }

  private void setReaderAndWriterIndex() {
    typeBuffer.readerIndex(0);
    typeBuffer.writerIndex(valueCount * TYPE_WIDTH);

    offsetBuffer.readerIndex(0);
    offsetBuffer.writerIndex(valueCount * OFFSET_WIDTH);
  }

  @Override
  @Deprecated
  public List<BufferBacked> getFieldInnerVectors() {
    throw new UnsupportedOperationException("There are no inner vectors. Use geFieldBuffers");
  }

  private String fieldName(byte typeId, MinorType type) {
    return type.name().toLowerCase() + typeId;
  }

  private FieldType fieldType(MinorType type) {
    return FieldType.nullable(type.getType());
  }

  public synchronized byte registerNewTypeId(Field field) {
    if (nextTypeId == typeFields.length) {
      throw new IllegalStateException("Dense union vector support at most " +
              typeFields.length + " relative types. Please use union of union instead");
    }
    byte typeId = nextTypeId;
    if (fieldType != null) {
      int[] typeIds = ((ArrowType.Union) fieldType.getType()).getTypeIds();
      if (typeIds != null) {
        int thisTypeId = typeIds[nextTypeId];
        if (thisTypeId > Byte.MAX_VALUE) {
          throw new IllegalStateException("Dense union vector types must be bytes. " + thisTypeId + " is too large");
        }
        typeId = (byte) thisTypeId;
      }
    }
    typeFields[typeId] = field;
    typeMapFields[nextTypeId] = typeId;
    this.nextTypeId += 1;
    return typeId;
  }

  private <T extends FieldVector> T addOrGet(byte typeId, MinorType minorType, Class<T> c) {
    return internalStruct.addOrGet(fieldName(typeId, minorType), fieldType(minorType), c);
  }

  private <T extends FieldVector> T addOrGet(byte typeId, MinorType minorType, ArrowType arrowType, Class<T> c) {
    return internalStruct.addOrGet(fieldName(typeId, minorType), FieldType.nullable(arrowType), c);
  }

  @Override
  public long getOffsetBufferAddress() {
    return offsetBuffer.memoryAddress();
  }

  @Override
  public long getDataBufferAddress() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getValidityBufferAddress() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ArrowBuf getValidityBuffer() { throw new UnsupportedOperationException(); }

  @Override
  public ArrowBuf getOffsetBuffer() { return offsetBuffer; }

  public ArrowBuf getTypeBuffer() { return typeBuffer; }

  @Override
  public ArrowBuf getDataBuffer() { throw new UnsupportedOperationException(); }

  public StructVector getStruct(byte typeId) {
    StructVector structVector = typeId < 0 ? null : (StructVector) childVectors[typeId];
    if (structVector == null) {
      int vectorCount = internalStruct.size();
      structVector = addOrGet(typeId, MinorType.STRUCT, StructVector.class);
      if (internalStruct.size() > vectorCount) {
        structVector.allocateNew();
        childVectors[typeId] = structVector;
        if (callBack != null) {
          callBack.doWork();
        }
      }
    }
    return structVector;
  }

  <#list vv.types as type>
    <#list type.minor as minor>
      <#assign name = minor.class?cap_first />
      <#assign fields = minor.fields!type.fields />
      <#assign uncappedName = name?uncap_first/>
      <#assign lowerCaseName = name?lower_case/>
      <#if !minor.typeParams?? || minor.class == "Decimal">

  public ${name}Vector get${name}Vector(byte typeId<#if minor.class == "Decimal">, ArrowType arrowType</#if>) {
    ValueVector vector = typeId < 0 ? null : childVectors[typeId];
    if (vector == null) {
      int vectorCount = internalStruct.size();
      vector = addOrGet(typeId, MinorType.${name?upper_case}<#if minor.class == "Decimal">, arrowType</#if>, ${name}Vector.class);
      childVectors[typeId] = vector;
      if (internalStruct.size() > vectorCount) {
        vector.allocateNew();
        if (callBack != null) {
          callBack.doWork();
        }
      }
    }
    return (${name}Vector) vector;
  }
      </#if>
    </#list>
  </#list>

  public ListVector getList(byte typeId) {
    ListVector listVector = typeId < 0 ? null : (ListVector) childVectors[typeId];
    if (listVector == null) {
      int vectorCount = internalStruct.size();
      listVector = addOrGet(typeId, MinorType.LIST, ListVector.class);
      if (internalStruct.size() > vectorCount) {
        listVector.allocateNew();
        childVectors[typeId] = listVector;
        if (callBack != null) {
          callBack.doWork();
        }
      }
    }
    return listVector;
  }

  public byte getTypeId(int index) {
    return typeBuffer.getByte(index * TYPE_WIDTH);
  }

  public ValueVector getVectorByType(byte typeId) {
    return typeId < 0 ? null : childVectors[typeId];
  }

  @Override
  public void allocateNew() throws OutOfMemoryException {
    /* new allocation -- clear the current buffers */
    clear();
    internalStruct.allocateNew();
    try {
      allocateTypeBuffer();
      allocateOffsetBuffer();
    } catch (Exception e) {
      clear();
      throw e;
    }
  }

  @Override
  public boolean allocateNewSafe() {
    /* new allocation -- clear the current buffers */
    clear();
    boolean safe = internalStruct.allocateNewSafe();
    if (!safe) { return false; }
    try {
      allocateTypeBuffer();
      allocateOffsetBuffer();
    } catch (Exception e) {
      clear();
      return  false;
    }

    return true;
  }

  private void allocateTypeBuffer() {
    typeBuffer = allocator.buffer(typeBufferAllocationSizeInBytes);
    typeBuffer.readerIndex(0);
    setNegative(0, typeBuffer.capacity());
  }

  private void allocateOffsetBuffer() {
    offsetBuffer = allocator.buffer(offsetBufferAllocationSizeInBytes);
    offsetBuffer.readerIndex(0);
    offsetBuffer.setZero(0, offsetBuffer.capacity());
  }


  @Override
  public void reAlloc() {
    internalStruct.reAlloc();
    reallocTypeBuffer();
    reallocOffsetBuffer();
  }

  public int getOffset(int index) {
    return offsetBuffer.getInt(index * OFFSET_WIDTH);
  }

  private void reallocTypeBuffer() {
    final long currentBufferCapacity = typeBuffer.capacity();
    long newAllocationSize = currentBufferCapacity * 2;
    if (newAllocationSize == 0) {
      if (typeBufferAllocationSizeInBytes > 0) {
        newAllocationSize = typeBufferAllocationSizeInBytes;
      } else {
        newAllocationSize = BaseValueVector.INITIAL_VALUE_ALLOCATION * TYPE_WIDTH * 2;
      }
    }

    newAllocationSize = CommonUtil.nextPowerOfTwo(newAllocationSize);
    assert newAllocationSize >= 1;

    if (newAllocationSize > BaseValueVector.MAX_ALLOCATION_SIZE) {
      throw new OversizedAllocationException("Unable to expand the buffer");
    }

    final ArrowBuf newBuf = allocator.buffer((int)newAllocationSize);
    newBuf.setBytes(0, typeBuffer, 0, currentBufferCapacity);
    typeBuffer.getReferenceManager().release(1);
    typeBuffer = newBuf;
    typeBufferAllocationSizeInBytes = (int)newAllocationSize;
    setNegative(currentBufferCapacity, newBuf.capacity() - currentBufferCapacity);
  }

  private void reallocOffsetBuffer() {
    final long currentBufferCapacity = offsetBuffer.capacity();
    long newAllocationSize = currentBufferCapacity * 2;
    if (newAllocationSize == 0) {
      if (offsetBufferAllocationSizeInBytes > 0) {
        newAllocationSize = offsetBufferAllocationSizeInBytes;
      } else {
        newAllocationSize = BaseValueVector.INITIAL_VALUE_ALLOCATION * OFFSET_WIDTH * 2;
      }
    }

    newAllocationSize = CommonUtil.nextPowerOfTwo(newAllocationSize);
    assert newAllocationSize >= 1;

    if (newAllocationSize > BaseValueVector.MAX_ALLOCATION_SIZE) {
      throw new OversizedAllocationException("Unable to expand the buffer");
    }

    final ArrowBuf newBuf = allocator.buffer((int) newAllocationSize);
    newBuf.setBytes(0, offsetBuffer, 0, currentBufferCapacity);
    newBuf.setZero(currentBufferCapacity, newBuf.capacity() - currentBufferCapacity);
    offsetBuffer.getReferenceManager().release(1);
    offsetBuffer = newBuf;
    offsetBufferAllocationSizeInBytes = (int) newAllocationSize;
  }

  @Override
  public void setInitialCapacity(int numRecords) { }

  @Override
  public int getValueCapacity() {
    long capacity = getTypeBufferValueCapacity();
    long offsetCapacity = getOffsetBufferValueCapacity();
    if (offsetCapacity < capacity) {
      capacity = offsetCapacity;
    }
    long structCapacity = internalStruct.getValueCapacity();
    if (structCapacity < capacity) {
      structCapacity = capacity;
    }
    return (int) capacity;
  }

  @Override
  public void close() {
    clear();
  }

  @Override
  public void clear() {
    valueCount = 0;
    typeBuffer.getReferenceManager().release();
    typeBuffer = allocator.getEmpty();
    offsetBuffer.getReferenceManager().release();
    offsetBuffer = allocator.getEmpty();
    internalStruct.clear();
  }

  @Override
  public void reset() {
    valueCount = 0;
    setNegative(0, typeBuffer.capacity());
    offsetBuffer.setZero(0, offsetBuffer.capacity());
    internalStruct.reset();
  }

  @Override
  public Field getField() {
    int childCount = (int) Arrays.stream(typeFields).filter(field -> field != null).count();
    List<org.apache.arrow.vector.types.pojo.Field> childFields = new ArrayList<>(childCount);
    int[] typeIds = new int[childCount];
    for (int i = 0; i < typeFields.length; i++) {
      if (typeFields[i] != null) {
        int curIdx = childFields.size();
        typeIds[curIdx] = i;
        childFields.add(typeFields[i]);
      }
    }

    FieldType fieldType;
    if (this.fieldType == null) {
      fieldType = FieldType.nullable(new ArrowType.Union(Dense, typeIds));
    } else {
      final UnionMode mode = UnionMode.Dense;
      fieldType = new FieldType(this.fieldType.isNullable(), new ArrowType.Union(mode, typeIds),
              this.fieldType.getDictionary(), this.fieldType.getMetadata());
    }

    return new Field(name, fieldType, childFields);
  }

  @Override
  public TransferPair getTransferPair(BufferAllocator allocator) {
    return getTransferPair(name, allocator);
  }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator) {
    return getTransferPair(ref, allocator, null);
  }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator, CallBack callBack) {
    return new org.apache.arrow.vector.complex.DenseUnionVector.TransferImpl(ref, allocator, callBack);
  }

  @Override
  public TransferPair makeTransferPair(ValueVector target) {
    return new TransferImpl((DenseUnionVector) target);
  }

  @Override
  public void copyFrom(int inIndex, int outIndex, ValueVector from) {
    Preconditions.checkArgument(this.getMinorType() == from.getMinorType());
    DenseUnionVector fromCast = (DenseUnionVector) from;
    int inOffset = fromCast.offsetBuffer.getInt(inIndex * OFFSET_WIDTH);
    fromCast.getReader().setPosition(inOffset);
    int outOffset = offsetBuffer.getInt(outIndex * OFFSET_WIDTH);
    getWriter().setPosition(outOffset);
    ComplexCopier.copy(fromCast.reader, writer);
  }

  @Override
  public void copyFromSafe(int inIndex, int outIndex, ValueVector from) {
    copyFrom(inIndex, outIndex, from);
  }

  public FieldVector addVector(byte typeId, FieldVector v) {
    String name = fieldName(typeId, v.getMinorType());
    Preconditions.checkState(internalStruct.getChild(name) == null, String.format("%s vector already exists", name));
    final FieldVector newVector = internalStruct.addOrGet(name, v.getField().getFieldType(), v.getClass());
    v.makeTransferPair(newVector).transfer();
    internalStruct.putChild(name, newVector);
    childVectors[typeId] = newVector;
    if (callBack != null) {
      callBack.doWork();
    }
    return newVector;
  }

  private class TransferImpl implements TransferPair {
    private final TransferPair[] internalTransferPairs = new TransferPair[nextTypeId];
    private final DenseUnionVector to;

    public TransferImpl(String name, BufferAllocator allocator, CallBack callBack) {
      to = new DenseUnionVector(name, allocator, null, callBack);
      internalStruct.makeTransferPair(to.internalStruct);
      createTransferPairs();
    }

    public TransferImpl(DenseUnionVector to) {
      this.to = to;
      internalStruct.makeTransferPair(to.internalStruct);
      createTransferPairs();
    }

    private void createTransferPairs() {
      for (int i = 0; i < nextTypeId; i++) {
        ValueVector srcVec = internalStruct.getVectorById(i);
        ValueVector dstVec = to.internalStruct.getVectorById(i);
        to.typeFields[i] = typeFields[i];
        to.typeMapFields[i] = typeMapFields[i];
        to.childVectors[i] = dstVec;
        internalTransferPairs[i] = srcVec.makeTransferPair(dstVec);
      }
    }

    @Override
    public void transfer() {
      to.clear();

      ReferenceManager refManager = typeBuffer.getReferenceManager();
      to.typeBuffer = refManager.transferOwnership(typeBuffer, to.allocator).getTransferredBuffer();

      refManager = offsetBuffer.getReferenceManager();
      to.offsetBuffer = refManager.transferOwnership(offsetBuffer, to.allocator).getTransferredBuffer();

      for (int i = 0; i < nextTypeId; i++) {
        if (internalTransferPairs[i] != null) {
          internalTransferPairs[i].transfer();
          to.childVectors[i] = internalTransferPairs[i].getTo();
        }
      }
      to.valueCount = valueCount;
      clear();
    }

    @Override
    public void splitAndTransfer(int startIndex, int length) {
      to.clear();

      // transfer type buffer
      int startPoint = startIndex * TYPE_WIDTH;
      int sliceLength = length * TYPE_WIDTH;
      ArrowBuf slicedBuffer = typeBuffer.slice(startPoint, sliceLength);
      ReferenceManager refManager = slicedBuffer.getReferenceManager();
      to.typeBuffer = refManager.transferOwnership(slicedBuffer, to.allocator).getTransferredBuffer();

      // transfer offset byffer
      while (to.offsetBuffer.capacity() < length * OFFSET_WIDTH) {
        to.reallocOffsetBuffer();
      }

      int [] typeCounts = new int[nextTypeId];
      int [] typeStarts = new int[nextTypeId];
      for (int i = 0; i < typeCounts.length; i++) {
        typeCounts[i] = 0;
        typeStarts[i] = -1;
      }

      for (int i = startIndex; i < startIndex + length; i++) {
        byte typeId = typeBuffer.getByte(i);
        to.offsetBuffer.setInt((i - startIndex) * OFFSET_WIDTH, typeCounts[typeId]);
        typeCounts[typeId] += 1;
        if (typeStarts[typeId] == -1) {
          typeStarts[typeId] = offsetBuffer.getInt(i * OFFSET_WIDTH);
        }
      }

      // transfer vector values
      for (int i = 0; i < nextTypeId; i++) {
        if (typeCounts[i] > 0 && typeStarts[i] != -1) {
          internalTransferPairs[i].splitAndTransfer(typeStarts[i], typeCounts[i]);
          to.childVectors[i] = internalTransferPairs[i].getTo();
        }
      }

      to.setValueCount(length);
    }

    @Override
    public ValueVector getTo() {
      return to;
    }

    @Override
    public void copyValueSafe(int from, int to) {
      this.to.copyFrom(from, to, DenseUnionVector.this);
    }
  }

  @Override
  public FieldReader getReader() {
    if (reader == null) {
      reader = new DenseUnionReader(this);
    }
    return reader;
  }

  public FieldWriter getWriter() {
    if (writer == null) {
      writer = new DenseUnionWriter(this);
    }
    return writer;
  }

  @Override
  public int getBufferSize() {
    return this.getBufferSizeFor(this.valueCount);
  }

  @Override
  public int getBufferSizeFor(final int count) {
    if (count == 0) {
      return 0;
    }
    return count * TYPE_WIDTH + count * OFFSET_WIDTH + DataSizeRoundingUtil.divideBy8Ceil(count)
        + internalStruct.getBufferSizeFor(count);
  }

  @Override
  public ArrowBuf[] getBuffers(boolean clear) {
    List<ArrowBuf> list = new java.util.ArrayList<>();
    setReaderAndWriterIndex();
    if (getBufferSize() != 0) {
      list.add(typeBuffer);
      list.add(offsetBuffer);
      list.addAll(java.util.Arrays.asList(internalStruct.getBuffers(clear)));
    }
    if (clear) {
      valueCount = 0;
      typeBuffer.getReferenceManager().retain();
      typeBuffer.close();
      typeBuffer = allocator.getEmpty();
      offsetBuffer.getReferenceManager().retain();
      offsetBuffer.close();
      offsetBuffer = allocator.getEmpty();
    }
    return list.toArray(new ArrowBuf[list.size()]);
  }

  @Override
  public Iterator<ValueVector> iterator() {
    List<ValueVector> vectors = org.apache.arrow.util.Collections2.toList(internalStruct.iterator());
    return vectors.iterator();
  }

  private ValueVector getVector(int index) {
    byte typeId = typeBuffer.getByte(index * TYPE_WIDTH);
    return getVectorByType(typeId);
  }

  public Object getObject(int index) {
    ValueVector vector = getVector(index);
    if (vector != null) {
      int offset = offsetBuffer.getInt(index * OFFSET_WIDTH);
      return vector.isNull(offset) ? null : vector.getObject(offset);
    }
    return null;
  }

  public void get(int index, DenseUnionHolder holder) {
    FieldReader reader = new DenseUnionReader(DenseUnionVector.this);
    reader.setPosition(index);
    holder.reader = reader;
  }

  public int getValueCount() {
    return valueCount;
  }

  /**
   * IMPORTANT: Union types always return non null as there is no validity buffer.
   *
   * To check validity correctly you must check the underlying vector.
   */
  public boolean isNull(int index) {
    return false;
  }

  @Override
  public int getNullCount() {
    return 0;
  }

  public int isSet(int index) {
    return isNull(index) ? 0 : 1;
  }

  DenseUnionWriter writer;

  public void setValueCount(int valueCount) {
    this.valueCount = valueCount;
    while (valueCount > getTypeBufferValueCapacity()) {
      reallocTypeBuffer();
      reallocOffsetBuffer();
    }
    setChildVectorValueCounts();
  }

  private void setChildVectorValueCounts() {
    int [] counts = new int[Byte.MAX_VALUE + 1];
    for (int i = 0; i < this.valueCount; i++) {
      byte typeId = getTypeId(i);
      if (typeId != -1) {
        counts[typeId] += 1;
      }
    }
    for (int i = 0; i < nextTypeId; i++) {
      childVectors[typeMapFields[i]].setValueCount(counts[typeMapFields[i]]);
    }
  }

  public void setSafe(int index, DenseUnionHolder holder) {
    FieldReader reader = holder.reader;
    if (writer == null) {
      writer = new DenseUnionWriter(DenseUnionVector.this);
    }
    int offset = offsetBuffer.getInt(index * OFFSET_WIDTH);
    MinorType type = reader.getMinorType();
    writer.setPosition(offset);
    byte typeId = holder.typeId;
    switch (type) {
      <#list vv.types as type>
        <#list type.minor as minor>
          <#assign name = minor.class?cap_first />
          <#assign fields = minor.fields!type.fields />
          <#assign uncappedName = name?uncap_first/>
          <#if !minor.typeParams?? || minor.class == "Decimal">
      case ${name?upper_case}:
      Nullable${name}Holder ${uncappedName}Holder = new Nullable${name}Holder();
      reader.read(${uncappedName}Holder);
      setSafe(index, ${uncappedName}Holder);
      break;
          </#if>
        </#list>
      </#list>
      case STRUCT:
      case LIST:  {
        setTypeId(index, typeId);
        ComplexCopier.copy(reader, writer);
        break;
      }
      default:
        throw new UnsupportedOperationException();
    }
  }
    <#list vv.types as type>
      <#list type.minor as minor>
        <#assign name = minor.class?cap_first />
        <#assign fields = minor.fields!type.fields />
        <#assign uncappedName = name?uncap_first/>
        <#if !minor.typeParams?? || minor.class == "Decimal">
  public void setSafe(int index, Nullable${name}Holder holder) {
    while (index >= getOffsetBufferValueCapacity()) {
      reallocOffsetBuffer();
    }
    byte typeId = getTypeId(index);
    ${name}Vector vector = get${name}Vector(typeId<#if minor.class == "Decimal">, new ArrowType.Decimal(holder.precision, holder.scale)</#if>);
    int offset = vector.getValueCount();
    vector.setValueCount(offset + 1);
    vector.setSafe(offset, holder);
    offsetBuffer.setInt(index * OFFSET_WIDTH, offset);
  }
        </#if>
      </#list>
    </#list>

  public void setTypeId(int index, byte typeId) {
    while (index >= getTypeBufferValueCapacity()) {
      reallocTypeBuffer();
    }
    typeBuffer.setByte(index * TYPE_WIDTH , typeId);
  }

  private int getTypeBufferValueCapacity() {
    return (int) typeBuffer.capacity() / TYPE_WIDTH;
  }

  private long getOffsetBufferValueCapacity() {
    return offsetBuffer.capacity() / OFFSET_WIDTH;
  }

  @Override
  public int hashCode(int index, ArrowBufHasher hasher) {
    if (isNull(index)) {
      return 0;
    }
    int offset = offsetBuffer.getInt(index * OFFSET_WIDTH);
    return getVector(index).hashCode(offset, hasher);
  }

  @Override
  public int hashCode(int index) {
    return hashCode(index, SimpleHasher.INSTANCE);
  }

  @Override
  public <OUT, IN> OUT accept(VectorVisitor<OUT, IN> visitor, IN value) {
    return visitor.visit(this, value);
  }

  @Override
  public String getName() {
    return name;
  }

  private void setNegative(long start, long end) {
    for (long i = start;i < end; i++) {
      typeBuffer.setByte(i, -1);
    }
  }
}

```


#### Error stacktrace:

```
scala.meta.internal.mtags.JavaToplevelMtags.unexpectedCharacter(JavaToplevelMtags.scala:352)
	scala.meta.internal.mtags.JavaToplevelMtags.parseToken$1(JavaToplevelMtags.scala:253)
	scala.meta.internal.mtags.JavaToplevelMtags.fetchToken(JavaToplevelMtags.scala:262)
	scala.meta.internal.mtags.JavaToplevelMtags.loop(JavaToplevelMtags.scala:73)
	scala.meta.internal.mtags.JavaToplevelMtags.indexRoot(JavaToplevelMtags.scala:42)
	scala.meta.internal.mtags.MtagsIndexer.index(MtagsIndexer.scala:21)
	scala.meta.internal.mtags.MtagsIndexer.index$(MtagsIndexer.scala:20)
	scala.meta.internal.mtags.JavaToplevelMtags.index(JavaToplevelMtags.scala:18)
	scala.meta.internal.mtags.Mtags.indexWithOverrides(Mtags.scala:74)
	scala.meta.internal.mtags.SymbolIndexBucket.indexSource(SymbolIndexBucket.scala:129)
	scala.meta.internal.mtags.SymbolIndexBucket.addSourceFile(SymbolIndexBucket.scala:108)
	scala.meta.internal.mtags.SymbolIndexBucket.$anonfun$addSourceJar$2(SymbolIndexBucket.scala:74)
	scala.collection.immutable.List.flatMap(List.scala:294)
	scala.meta.internal.mtags.SymbolIndexBucket.$anonfun$addSourceJar$1(SymbolIndexBucket.scala:70)
	scala.meta.internal.io.PlatformFileIO$.withJarFileSystem(PlatformFileIO.scala:79)
	scala.meta.internal.io.FileIO$.withJarFileSystem(FileIO.scala:33)
	scala.meta.internal.mtags.SymbolIndexBucket.addSourceJar(SymbolIndexBucket.scala:68)
	scala.meta.internal.mtags.OnDemandSymbolIndex.$anonfun$addSourceJar$2(OnDemandSymbolIndex.scala:85)
	scala.meta.internal.mtags.OnDemandSymbolIndex.tryRun(OnDemandSymbolIndex.scala:131)
	scala.meta.internal.mtags.OnDemandSymbolIndex.addSourceJar(OnDemandSymbolIndex.scala:84)
	scala.meta.internal.metals.Indexer.indexJar(Indexer.scala:565)
	scala.meta.internal.metals.Indexer.addSourceJarSymbols(Indexer.scala:559)
	scala.meta.internal.metals.Indexer.$anonfun$indexDependencySources$5(Indexer.scala:387)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:619)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:617)
	scala.collection.AbstractIterable.foreach(Iterable.scala:935)
	scala.collection.IterableOps$WithFilter.foreach(Iterable.scala:905)
	scala.meta.internal.metals.Indexer.$anonfun$indexDependencySources$1(Indexer.scala:378)
	scala.meta.internal.metals.Indexer.$anonfun$indexDependencySources$1$adapted(Indexer.scala:377)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:619)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:617)
	scala.collection.AbstractIterable.foreach(Iterable.scala:935)
	scala.meta.internal.metals.Indexer.indexDependencySources(Indexer.scala:377)
	scala.meta.internal.metals.Indexer.$anonfun$indexWorkspace$20(Indexer.scala:198)
	scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
	scala.meta.internal.metals.TimerProvider.timedThunk(TimerProvider.scala:25)
	scala.meta.internal.metals.Indexer.$anonfun$indexWorkspace$19(Indexer.scala:191)
	scala.meta.internal.metals.Indexer.$anonfun$indexWorkspace$19$adapted(Indexer.scala:187)
	scala.collection.immutable.List.foreach(List.scala:334)
	scala.meta.internal.metals.Indexer.indexWorkspace(Indexer.scala:187)
	scala.meta.internal.metals.Indexer.$anonfun$profiledIndexWorkspace$2(Indexer.scala:57)
	scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
	scala.meta.internal.metals.TimerProvider.timedThunk(TimerProvider.scala:25)
	scala.meta.internal.metals.Indexer.$anonfun$profiledIndexWorkspace$1(Indexer.scala:57)
	scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
	scala.concurrent.Future$.$anonfun$apply$1(Future.scala:687)
	scala.concurrent.impl.Promise$Transformation.run(Promise.scala:467)
	java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
	java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
	java.base/java.lang.Thread.run(Thread.java:833)
```
#### Short summary: 

Java indexer failed with and exception.