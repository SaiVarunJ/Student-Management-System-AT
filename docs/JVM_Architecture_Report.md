# JVM Architecture Report

This document explains the major components of the Java Virtual Machine (JVM), describes how bytecode is executed, compares the Just-In-Time (JIT) compiler and the interpreter, and explains the "Write Once, Run Anywhere" (WORA) principle.

---

## 1. High-level diagram of JVM components

Below are two representations: a mermaid flowchart (works in many renderers like GitHub/GitLab) and an ASCII fallback.

Mermaid diagram:

```mermaid
flowchart LR
  subgraph CL [Class Loader Subsystem]
    direction TB
    CLB(Bootstrap Loader)
    CLE(Extension Loader)
    CLA(Application / System Loader)
  end

  subgraph RDA [Runtime Data Areas]
    direction TB
    MA(Method Area)
    Heap(Heap)
    JVMStk(JVM Stacks)
    PC(Program Counters)
    NMS(Native Method Stacks)
  end

  subgraph EE [Execution Engine]
    direction TB
    Interp(Interpreter)
    JIT[JIT Compiler]
    GC(Garbage Collector)
    NIF(Native Interface)
  end

  CL --> RDA
  CL --> EE
  RDA --> EE
  EE --> NIF
  classDef[".class files / bytecode"] --> CL
  runtimeApp["Running Application"] --> EE
```

ASCII fallback:

```
           +----------------------+          +----------------------+
           |  .class files / JAR  |          |  Native Libraries    |
           +----------+-----------+          +----------+-----------+
                      |                                ^
                      v                                |
               +--------------+                        |
               | Class Loader |------------------------+
               | subsystem    | (loads, links, init)    |
               +------+-------+                        |
                      |                                |
                      v                                |
        +----------------------------------------------+|
        |             Runtime Data Areas               ||
        |  - Method Area   - Heap   - JVM Stacks       ||
        |  - PC Registers  - Native Stacks             ||
        +-----------------+----------------------------+|
                          |                             |
                          v                             v
                   +----------------------+        +------------------+
                   |  Execution Engine    |<------>| Native Interface |
                   |  - Interpreter       |        +------------------+
                   |  - JIT Compiler      |
                   |  - Garbage Collector |
                   +----------------------+
```

---

## 2. Class Loader Subsystem

Responsibilities:
- Locate and read binary class data (.class files or entries in JARs).
- Create an in-memory representation of the class (class object in the Method Area).
- Perform linking (verification, preparation, resolution) and initialization.

Key loaders (typical hierarchy):
- Bootstrap (primordial) Class Loader: loads core Java runtime classes (rt.jar or java.base module).
- Extension (Platform) Class Loader: loads optional extension libraries.
- Application (System) Class Loader: loads classes from the application classpath.

Lifecycle steps:
1. Loading: Find and read class bytes.
2. Verification: Check bytecode correctness and safety.
3. Preparation: Allocate class-level data (static fields with default values) in the Method Area.
4. Resolution: Convert symbolic references to direct references (may be lazy).
5. Initialization: Run static initializers and assign explicit static values.

Notes: custom class loaders can alter visibility and allow multiple versions of the same class to coexist.

---

## 3. Runtime Data Areas (Memory Model)

The JVM specifies several runtime data areas, some are per-JVM and some per-thread.

- Method Area (shared): stores class structures such as runtime constant pool, field and method data, and code for methods and constructors. Often implemented within metaspace (modern HotSpot) or PermGen (older VMs).
- Heap (shared): runtime allocation area for all class instances and arrays. This is the primary area managed by the Garbage Collector (GC).
- JVM Stacks (per thread): each Java thread has its own stack. A stack stores frames, and each frame contains local variables, operand stack, and a reference to the runtime constant pool of the class of the current method.
- Program Counter (PC) Register (per thread): holds the address of the currently executing instruction in the current frame. For native methods, the PC may be undefined.
- Native Method Stacks (per thread): used for native (JNI) method invocation. Some JVMs implement native stacks as part of the same area as Java stacks.

Memory management and GC work primarily on the Heap, with interactions from the Method Area (for class metadata) and roots coming from stacks and static fields.

---

## 4. Execution Engine

Components:
- Interpreter: reads bytecode instructions and executes them directly. Good for fast startup and low memory footprint but slower steady-state throughput.
- JIT Compiler: compiles frequently executed bytecode sequences (hot methods) into native machine code at runtime. Optimizes using runtime profiling (inlining, escape analysis, loop optimizations).
- Garbage Collector: reclaims unused objects from the heap. Multiple GC algorithms exist (Serial, Parallel, CMS, G1, ZGC, Shenandoah), each with trade-offs in throughput vs pause times.
- Native Interface: Java Native Interface (JNI) allows calling platform-native code and libraries.

How execution works in modern JVMs:
- Initially, code is executed by the interpreter.
- A profiler counts method/instruction execution frequency.
- Hot methods/loops are handed to the JIT which compiles and replaces the interpreted path with optimized native code.
- JIT uses runtime information (type profiles, branch probabilities) to generate more aggressive optimizations than static compilers.

---

## 5. JIT Compiler vs Interpreter (comparison)

Contract (inputs/outputs, error modes):
- Inputs: JVM bytecode (class files), runtime profiling data.
- Outputs: executed program behavior (side effects, return values), optionally native machine code cached in memory.
- Error modes: verification/linking failures, JIT bugs that can cause incorrect native code (rare but possible), runtime exceptions.

Interpreter:
- Pros: immediate execution (low startup overhead), simple implementation, easy debugging (one-to-one mapping to bytecode). 
- Cons: per-instruction overhead; no heavy cross-call optimizations.

JIT Compiler:
- Pros: high steady-state performance, runtime-driven optimizations, inlining across calls, speculative optimizations based on actual types.
- Cons: compilation overhead (CPU/memory), warm-up time before reaching peak performance, complexity (can introduce uncommon runtime bugs in rare JVMs).

Typical strategy: tiered compilation (interpreted -> C1 quick compiler -> C2 optimizing compiler) to balance startup and steady-state performance.

When to prefer one over the other:
- Short-lived programs or simple scripts may benefit from interpretation (faster to start).
- Long-running server applications benefit from JIT optimizations.

---

## 6. Bytecode execution process (step-by-step)

1. Java source (.java) is compiled by javac into platform-neutral bytecode (.class).
2. At runtime, the Class Loader subsystem locates .class bytes and loads them into the JVM.
3. The bytecode verifier checks that the code adheres to bytecode and JVM safety constraints (stack heights, type correctness, access control).
4. The class is linked: symbolic references are resolved (either eagerly or lazily), and static fields are prepared.
5. Class initialization runs static initializers (<clinit>) and assigns explicit static values.
6. The Execution Engine interprets bytecode instructions. As the program runs, the profiler marks hot spots.
7. The JIT compiler compiles hot methods/paths to native code. The execution engine replaces interpreted frames or dispatch with compiled code.
8. Garbage collector runs periodically or concurrently, reclaiming memory in the Heap while preserving program semantics. GC uses root scanning (from stacks, static fields, registers) to determine live objects.
9. If native code is required, the Native Interface handles calling out to platform-native libraries.

Important details:
- Exceptions, synchronization, and class metadata accesses are all implemented in the runtime and impact both interpreted and JIT-compiled code.
- Many JVMs perform deoptimization: if a JIT makes speculative assumptions and they are invalidated at runtime, the JVM can fall back to a safe, deoptimized (interpreted) state and recompile with new information.

---

## 7. "Write Once, Run Anywhere" (WORA)

Meaning:
- Java source is compiled to JVM bytecode which is independent of the underlying CPU and OS.
- Any platform that provides a conformant JVM implementation can execute the same bytecode without recompilation.

How it works:
- Bytecode abstracts machine instructions and relies on the JVM to provide a consistent execution model and standard libraries.
- The JVM implementation maps bytecode to native instructions for the host platform. This mapping is the responsibility of the JVM vendor and hides platform differences from the bytecode.

Caveats and practical limitations:
- Native code dependency: if an application uses JNI or platform-specific libraries, those native components must be recompiled or replaced for the target platform.
- JVM differences: subtle behavioral differences, different default GC algorithms, or JDK versions can cause different performance or timing; strict specification compliance reduces incompatibilities but does not eliminate them.
- File system, line endings, and locale differences: code that assumes platform specifics (file paths, encodings) may not be portable without care.

In practice: WORA is highly effective for pure Java applications that avoid native bindings and adhere to standard APIs.

---

## 8. Short glossary

- Bytecode: platform-neutral compiled form of Java source.
- Class Loader: component that loads class byte arrays into the JVM and prepares them for execution.
- Method Area: JVM memory area holding class metadata and code structures.
- Heap: area for object allocation and GC.
- JIT: runtime compiler that produces native machine code from bytecode.
- Interpreter: executes bytecode instruction-by-instruction.
- JNI: Java Native Interface for calling native code.

---

## 9. Requirements coverage checklist

- Create detailed diagram of JVM components: Done (mermaid + ASCII).
- Explain JIT compiler vs Interpreter: Done (section 5, comparison and trade-offs).
- Demonstrate understanding of bytecode execution process: Done (section 6, step-by-step).
- Write explanation of "Write Once, Run Anywhere": Done (section 7, with caveats).

---

## Next steps / improvements
- Add hand-drawn or generated images (PNG/SVG) to `docs/image/` for prettier diagrams.
- Add references and links to Oracle/OpenJDK JVM specification and HotSpot internals for deeper reading.

---

## References

- Java Virtual Machine Specification (Oracle) - https://docs.oracle.com/javase/specs/
- Java Language Specification - https://docs.oracle.com/javase/specs/jls/
- OpenJDK HotSpot Internals - https://openjdk.org/groups/hotspot/
- HotSpot Garbage Collection Tuning Guide - https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/
- GraalVM (advanced JIT / AOT) - https://www.graalvm.org/
