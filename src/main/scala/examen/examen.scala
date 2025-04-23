package examen

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

object examen {

  /**
   * Ejercicio 1: Crear un DataFrame y realizar operaciones básicas
   * 
   * Pregunta: Crea un DataFrame a partir de una secuencia de tuplas que contenga información sobre
   *           estudiantes (nombre, edad, calificación).
   *         Realiza las siguientes operaciones:
   *          Muestra el esquema del DataFrame.
   *          Filtra los estudiantes con una calificación mayor a 8.
   *          Selecciona los nombres de los estudiantes y ordénalos por calificación de forma descendente.
   */
 def ejercicio1(estudiantes: DataFrame): DataFrame = {
  estudiantes.printSchema()
  estudiantes
    .orderBy(desc("calificacion"))
    .select("nombre")
}

  /**
   * Ejercicio 2: UDF (User Defined Function)
   * 
   * Pregunta: Define una función que determine si un número es par o impar.
   *           Aplica esta función a una columna de un DataFrame que contenga una lista de números.
   */
  def ejercicio2(numeros: DataFrame): DataFrame = {
    val parImparUDF = udf((numero: Int) => {
      if (numero % 2 == 0) "Par" else "Impar"
    })
    
    numeros.select(parImparUDF(col("numero")).as("paridad"))
  }

  /**
   * Ejercicio 3: Joins y agregaciones
   * 
   * Pregunta: Dado dos DataFrames,
   *           uno con información de estudiantes (id, nombre)
   *           y otro con calificaciones (id_estudiante, asignatura, calificacion),
   *           realiza un join entre ellos y calcula el promedio de calificaciones por estudiante.
   */
  def ejercicio3(estudiantes: DataFrame, calificaciones: DataFrame): DataFrame = {
    val joinedDF = estudiantes.join(
      calificaciones,
      estudiantes("id") === calificaciones("id_estudiante"),
      "inner"
    )
    
    joinedDF
      .groupBy("id", "nombre")
      .agg(
        avg("calificacion").alias("promedio_calificaciones")
      )
      .orderBy("id")
  }

  /**
   * Ejercicio 4: Uso de RDDs
   * 
   * Pregunta: Crea un RDD a partir de una lista de palabras y 
   *           cuenta la cantidad de ocurrencias de cada palabra.
   */
  def ejercicio4(palabras: List[String])(implicit spark: SparkSession): RDD[(String, Int)] = {
    val palabrasRDD = spark.sparkContext.parallelize(palabras)
    
    palabrasRDD
      .map(palabra => (palabra, 1))
      .reduceByKey(_ + _)
      .sortBy(_._2, ascending = false)
  }

  /**
   * Ejercicio 5: Procesamiento de archivos
   * 
   * Pregunta: Carga un archivo CSV que contenga información sobre
   *           ventas (id_venta, id_producto, cantidad, precio_unitario)
   *           y calcula el ingreso total (cantidad * precio_unitario) por producto.
   */
  def ejercicio5(ventas: DataFrame): DataFrame = {
    ventas
      .withColumn("ingreso", col("cantidad").cast("int") * col("precio_unitario").cast("double"))
      .groupBy("id_producto")
      .agg(
        sum("ingreso").alias("ingreso_total")
      )
  }
}
