USE guali;

INSERT INTO leccion (titulo, contenido_leccion, objetivo) VALUES
                                                              ('leccion-01-hola-mundo',
                                                               '# Hola, mundo

                                                               En esta lección vas a escribir tu primer programa en Java.

                                                               Copia el siguiente código en el editor, **escribiéndolo tú mismo**:

                                                               ```java
                                                               void main() {
                                                                   IO.println("¡Hola, Mundo!");
                                                               }
                                                               ```

                                                               Cuando lo tengas, pulsa **Compilar** y luego **Ejecutar**.',
                                                               'EJECUTAR_EXITOSO'),

                                                              ('leccion-02-errores-compilacion',
                                                               '# Errores de compilación

                                                               El compilador te avisa cuando el código tiene errores de sintaxis.

                                                               Escribe este código tal cual. Le falta algo a propósito:

                                                               ```java
                                                               void main() {
                                                                   IO.println("Hola")
                                                               }
                                                               ```

                                                               Pulsa **Compilar** y lee el mensaje de la consola. ¿Qué te dice que falta?',
                                                               'COMPILAR_CON_ERROR');