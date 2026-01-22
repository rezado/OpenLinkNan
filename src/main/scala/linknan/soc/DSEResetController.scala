package linknan.soc

import chisel3._
import chisel3.util._
import zhujiang._
import freechips.rocketchip.diplomacy.LazyRawModuleImp
import org.chipsalliance.cde.config.Parameters
import zhujiang.ZJRawModule
import chisel3.Bundle
import chisel3.AsyncReset
import chisel3.util.experimental.BoringUtils

class DSEResetController(implicit p: Parameters) extends ZJRawModule with ImplicitClock with ImplicitReset {
    override protected val implicitClock = Wire(Clock())
    override protected val implicitReset = Wire(AsyncReset())

    val io = IO(new Bundle {
        val clock = Input(Clock())
        val reset = Input(AsyncReset())
        val ctrlSel = Input(Bool())
        val instrCnt = Input(UInt(64.W))
        val max_instr_cnt = Input(UInt(64.W))
        val reset_valid = Output(Bool())
        val reset_vector = Output(UInt(raw.W))
        val enable_collect_perf = Output(Bool())
    })

    val commit_valid = WireInit(false.B)
    BoringUtils.addSink(commit_valid, "DSE_COMMITVALID")


    // core reset generation
    implicitClock := io.clock
    implicitReset := io.reset.asAsyncReset
    withClockAndReset(implicitClock, implicitReset) {
      val coreResetReg = RegInit(false.B)
      val resetVectorReg = RegInit(0x10000000.U(raw.W))

      // driver -> workload reset
      val ctrlSelDelayed = RegNext(io.ctrlSel)
      val ctrlSelChanged = (ctrlSelDelayed =/= io.ctrlSel)
      val lastCycleCommit = RegNext(commit_valid)
      val ctrlSelChangedStall = RegInit(false.B)
      val ctrlSelReset = ctrlSelChangedStall && lastCycleCommit
      when (ctrlSelChanged) {
        ctrlSelChangedStall := true.B
      }

      val reset_avoid = RegInit(false.B)
      val (reset_avoid_counter, reset_avoid_end) = Counter(coreResetReg, 256)
      when (reset_avoid_end) {
        reset_avoid := false.B
      }

      // workload -> driver reset
      val reach_instr_limit = (io.instrCnt >= io.max_instr_cnt) && (resetVectorReg === 0x80000000L.U) && !reset_avoid
      io.enable_collect_perf := (io.instrCnt >= io.max_instr_cnt - 10.U) && (resetVectorReg === 0x80000000L.U) && !reset_avoid

      when (ctrlSelReset) {
        coreResetReg := true.B
        resetVectorReg := 0x80000000L.U
        ctrlSelChangedStall := false.B
        reset_avoid := true.B
      }

      when (reach_instr_limit) {
        coreResetReg := true.B
        resetVectorReg := 0x10000000L.U
      }

      // core reset counter
      val (core_rst_counter, core_rst_end) = Counter(coreResetReg, 256)
      when (core_rst_end) {
        coreResetReg := false.B
      }

      io.reset_valid := coreResetReg
      io.reset_vector := resetVectorReg
    }
}