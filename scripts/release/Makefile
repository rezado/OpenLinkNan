ABS_WORK_DIR = $(shell pwd)
RELEASE_DIR = $(shell find $(ABS_WORK_DIR) -name "Release-LinkNan*")
SIM_DIR = $(ABS_WORK_DIR)/sim
NEMU_DIR = $(ABS_WORK_DIR)/NEMU
CASES_DIR = $(ABS_WORK_DIR)/cases

CFG_CIR           = $(ABS_WORK_DIR)/env/difftest/config
SIM_CSRC_DIR 	  = $(ABS_WORK_DIR)/env/difftest/src/test/csrc/common
PLUGIN_CHEAD_DIR  = $(ABS_WORK_DIR)/env/difftest/src/test/csrc/plugin/include
PLUGIN_CSRC_DIR   = $(ABS_WORK_DIR)/env/difftest/src/test/csrc/plugin/spikedasm
DIFFTEST_CSRC_DIR = $(ABS_WORK_DIR)/env/difftest/src/test/csrc/difftest
VCS_CSRC_DIR 	  = $(ABS_WORK_DIR)/env/difftest/src/test/csrc/vcs
GEN_CSRC_DIR      = $(ABS_WORK_DIR)/env/generated-src

SIM_COMMON_DIR    = $(ABS_WORK_DIR)/env/difftest/src/test/vsrc/common
SIM_ST_DIR        = $(ABS_WORK_DIR)/env/difftest/src/test/vsrc/st
VCS_TOP_DIR       = $(ABS_WORK_DIR)/env/difftest/src/test/vsrc/vcs
SIMTOP_DIR        = $(ABS_WORK_DIR)/env/sim

# VCS compile
ifndef VERDI_HOME
$(error VERDI_HOME is not set. Try whereis verdi, abandon /bin/verdi and set VERID_HOME manually)
else
NOVAS_HOME = $(VERDI_HOME)
NOVAS = $(NOVAS_HOME)/share/PLI/VCS/LINUX64
EXTRA += +define+CONSIDER_FSDB -P $(NOVAS)/novas.tab $(NOVAS)/pli.a
endif

VCS_FLAGS += -full64 +v2k -timescale=1ns/10ps -sverilog -debug_access+all +lint=TFIPC-L
VCS_FLAGS += -l vcs.log -top tb_top -fgp -lca -kdb +nospecify +notimingcheck -no_save -xprop
VCS_FLAGS += +define+DIFFTEST +define+ASSERT_VERBOSE_COND_=1 +define+PRINTF_COND_=1
VCS_FLAGS += +define+STOP_COND_=1 +define+VCS +incdir+$(GEN_CSRC_DIR)
VCS_FLAGS += -CFLAGS "$(VCS_CXXFLAGS)" -LDFLAGS "$(VCS_LDFLAGS)" -j200
VCS_FLAGS += $(EXTRA)

VCS_CXXFLAGS += -std=c++17 -static -Wall -DREF_PROXY=NemuProxy -DNUM_CORES=4
VCS_CXXFLAGS += -I$(CFG_CIR) -I$(GEN_CSRC_DIR) -I$(VCS_CSRC_DIR) -I$(SIM_CSRC_DIR)
VCS_CXXFLAGS += -I$(PLUGIN_CHEAD_DIR) -I$(PLUGIN_CSRC_DIR) -I$(DIFFTEST_CSRC_DIR)
VCS_LDFLAGS += -Wl,--no-as-needed -lpthread -lSDL2 -ldl -lz -lzstd


DUT_FILELIST = $(SIM_DIR)/dut.f
ENV_FILELIST = $(SIM_DIR)/env.f

FORCE:

$(DUT_FILELIST): FORCE
	@mkdir -p $(SIM_DIR)
	@cat $(RELEASE_DIR)/bosc_FullSys.f \
		 | sort -u > $(DUT_FILELIST)
	sed -i 's|$$release_path|$(RELEASE_DIR)|g' $(DUT_FILELIST)

$(ENV_FILELIST): FORCE
	@mkdir -p $(SIM_DIR)
	@find $(SIM_CSRC_DIR) -name "*.cpp" > $(ENV_FILELIST)
	@find $(PLUGIN_CSRC_DIR) -name "*.cpp" >> $(ENV_FILELIST)
	@find $(DIFFTEST_CSRC_DIR) -name "*.cpp" >> $(ENV_FILELIST)
	@find $(GEN_CSRC_DIR) -name "*.cpp" >> $(ENV_FILELIST)
	@find $(VCS_CSRC_DIR) -name "*.cpp" -or -name "*.c" >> $(ENV_FILELIST)
	@find $(SIM_COMMON_DIR) -name "*.v" -or -name "*.sv" >> $(ENV_FILELIST)
	@find $(SIM_ST_DIR) -name "*.v" -or -name "*.sv" >> $(ENV_FILELIST)
	@find $(SIMTOP_DIR) -name "*.v" -or -name "*.sv" >> $(ENV_FILELIST)
	@find $(VCS_TOP_DIR) -name "*.v" -or -name "*.sv" >> $(ENV_FILELIST)


simv: $(DUT_FILELIST) $(ENV_FILELIST)
	@mkdir -p $(SIM_DIR)/comp
	cd $(SIM_DIR)/comp && vcs $(VCS_FLAGS) -f $(DUT_FILELIST) -f $(ENV_FILELIST)
	rm $(ENV_FILELIST)

# VCS simulation
libnemu := $(NEMU_DIR)/build/riscv64-nemu-interpreter-so

$(libnemu):
	NEMU_HOME=$(NEMU_DIR) make -C $(NEMU_DIR) riscv64-nhv5-multi-ref_defconfig
	NEMU_HOME=$(NEMU_DIR) make -C $(NEMU_DIR) -j

RUN_BIN_DIR = $(CASES_DIR)
RUN_BIN  ?= dhrystone.bin
RUN_OPTS += -fgp=num_threads:4,num_fsdb_threads:4
RUN_OPTS += -assert finish_maxfail=30 -assert global_finish_maxfail=10000
RUN_OPTS += +dump-wave=fsdb +workload=$(RUN_BIN_DIR)/$(RUN_BIN)
RUN_OPTS += $(if $(filter 0,$(DIFF)),+no-diff,+diff=$(libnemu))

run: $(libnemu)
	@mkdir -p $(SIM_DIR)/$(RUN_BIN)
	@touch $(SIM_DIR)/$(RUN_BIN)/sim.log
	@rm -f $(SIM_DIR)/$(RUN_BIN)/simv
	@rm -rf $(SIM_DIR)/$(RUN_BIN)/simv.daidir
	@ln -s $(SIM_DIR)/comp/simv $(SIM_DIR)/$(RUN_BIN)/simv
	@ln -s $(SIM_DIR)/comp/simv.daidir $(SIM_DIR)/$(RUN_BIN)/simv.daidir
	cd $(SIM_DIR)/$(RUN_BIN) && (./simv $(RUN_OPTS) 2> assert.log | tee sim.log)

clean:
	rm -rf $(SIM_DIR)
	make -C NEMU clean
	make -C NEMU clean-softfloat


##########################
## Verilator simulation ##
##########################

VSIM_DIR := $(ABS_WORK_DIR)/vsim
VSIM_COMP_DIR := $(VSIM_DIR)/comp

VRLT_CSRC_DIR = $(ABS_WORK_DIR)/env/difftest/src/test/csrc/verilator

VRLT_CFLAGS := -std=c++17 -DVERILATOR -DNUM_CORES=4
VRLT_CFLAGS += -DREF_PROXY=NemuProxy -DEMU_THREAD=16
VRLT_CFLAGS += -I$(CFG_CIR) -I$(GEN_CSRC_DIR) -I$(VRLT_CSRC_DIR) -I$(SIM_CSRC_DIR)
VRLT_CFLAGS += -I$(PLUGIN_CHEAD_DIR) -I$(PLUGIN_CSRC_DIR) -I$(DIFFTEST_CSRC_DIR)

VRLT_LDFLAGS := -ldl -lrt -lpthread -lsqlite3 -lz -lzstd

VRLT_FLAGS := --exe --cc --top-module SimTop --assert --x-assign unique
VRLT_FLAGS += -Wno-UNOPTTHREADS -Wno-STMTDLY -Wno-WIDTH --no-timing 
VRLT_FLAGS += --output-split 50000 -output-split-cfuncs 50000 --inline-mult 5000 
VRLT_FLAGS += --threads 16 --threads-dpi all --trace
VRLT_FLAGS += +define+DIFFTEST +define+VERILATOR=1 +define+PRINTF_COND=1 
VRLT_FLAGS += +define+RANDOMIZE_REG_INIT +define+RANDOMIZE_MEM_INIT
VRLT_FLAGS += +define+RANDOMIZE_GARBAGE_ASSIGN +define+RANDOMIZE_DELAY=0
VRLT_FLAGS += -CFLAGS "$(VRLT_CFLAGS)" -LDFLAGS "$(VRLT_LDFLAGS)" -I$(GEN_CSRC_DIR)
VRLT_FLAGS += -Mdir $(VSIM_COMP_DIR) -f $(DUT_FILELIST) -f $(ENV_FILELIST)
VRLT_FLAGS += -o $(VSIM_COMP_DIR)/emu

VRLT_MK_FLAGS = VM_PARALLEL_BUILDS=1 OPT_FAST=-O3 -f VSimTop.mk

emu: $(DUT_FILELIST) $(ENV_FILELIST)
	@find $(VRLT_CSRC_DIR) -name "*.cpp" >> $(ENV_FILELIST)
	@mkdir -p $(VSIM_COMP_DIR)
	verilator $(VRLT_FLAGS)
	cd $(VSIM_COMP_DIR) && make VM_PARALLEL_BUILDS=1 OPT_FAST=-O3 -f $(VSIM_COMP_DIR)/VSimTop.mk -j16

VRLT_RUN_OPTS := -s 1234 --wave-path $(RUN_BIN_DIR)/$(RUN_BIN)/wave.vcd
VRLT_RUN_OPTS += -i $(RUN_BIN_DIR)/$(RUN_BIN)
VRLT_RUN_OPTS += $(if $(filter 0,$(DIFF)),--no-diff,--diff=$(libnemu))
VRLT_RUN_OPTS += $(if $(filter 1,$(QUAL)),-F $(RUN_BIN_DIR)/flash.bin,)
VRLT_RUN_OPTS += $(if $(filter 1,$(DUMP)),--dump-wave, --enable-fork -X 50)


emu-run: $(libnemu)
	@mkdir -p $(VSIM_DIR)/$(RUN_BIN)
	@touch $(VSIM_DIR)/$(RUN_BIN)/sim.log
	@rm -f $(VSIM_DIR)/$(RUN_BIN)/emu
	@ln -s $(VSIM_DIR)/comp/emu $(VSIM_DIR)/$(RUN_BIN)/emu
	cd $(VSIM_DIR)/$(RUN_BIN) && (./emu $(VRLT_RUN_OPTS) 2> assert.log | tee sim.log)

########################
## Xcelium simulation ##
########################

XSIM_DIR := $(ABS_WORK_DIR)/xsim
XSIM_COMP_DIR := $(XSIM_DIR)/comp

XRUN_CFLAGS := -DNUM_CORES=4 -DREF_PROXY=NemuProxy -DEMU_THREAD=16
XRUN_CFLAGS += -I$(CFG_CIR) -I$(GEN_CSRC_DIR) -I$(VCS_CSRC_DIR) -I$(SIM_CSRC_DIR)
XRUN_CFLAGS += -I$(PLUGIN_CHEAD_DIR) -I$(PLUGIN_CSRC_DIR) -I$(DIFFTEST_CSRC_DIR)

XRUN_FLAGS := -64bit -elaborate -sv -timescale 1ns/10ps -l xelab.log -access +rwc
XRUN_FLAGS += -top tb_top -nospecify -xprop C
XRUN_FLAGS += -define PRINTF_COND=1 -define XCELIUM -define CONSIDER_FSDB -define SIM_TOP_MODULE_NAME=tb_top.sim
XRUN_FLAGS += -incdir $(GEN_CSRC_DIR)
XRUN_FLAGS += $(XRUN_CFLAGS)
XRUN_FLAGS += -f $(DUT_FILELIST) -f $(ENV_FILELIST)
XRUN_FLAGS += -loadpli1 $(NOVAS_HOME)/share/PLI/IUS/LINUX64/boot/debpli.so:novas_pli_boot

check_env:
	@if [ -z "$(VERDI_HOME)" ]; then \
		echo "VERDI_HOME is not set. Please set VERDI_HOME to your Cadence installation directory."; \
		exit 1; \
	fi

xsim: $(DUT_FILELIST) $(ENV_FILELIST) check_env
	@mkdir -p $(XSIM_COMP_DIR)
	cd $(XSIM_COMP_DIR) && \
	LD_LIBRARY_PATH="$(VERDI_HOME)/share/PLI/lib/linux64:$(VERDI_HOME)/share/PLI/IUS/linux64:$(VERDI_HOME)/share/PLI/IUS/linux64/boot:$(LD_LIBRARY_PATH)" \
	xrun $(XRUN_FLAGS)


XRUN_OPTS := -R -l xrun.log
XRUN_OPTS += +dump-wave=fsdb +max-cycles=0 +workload=$(RUN_BIN_DIR)/$(RUN_BIN)
XRUN_OPTS += $(if $(filter 1,$(DIFF)),+diff=$(libnemu), +no-diff)
XRUN_OPTS += $(if $(filter 1,$(QUAL)),+diff=$(libnemu), +no-diff)
XRUN_OPTS += -loadpli1 $(NOVAS_HOME)/share/PLI/IUS/LINUX64/boot/debpli.so:novas_pli_boot
xrun: check_env
	@mkdir -p $(XSIM_DIR)/$(RUN_BIN)
	@rm -rf $(XSIM_DIR)/$(RUN_BIN)/xcelium.d
	@ln -s $(XSIM_DIR)/comp/xcelium.d $(XSIM_DIR)/$(RUN_BIN)/xcelium.d
	cd $(XSIM_DIR)/$(RUN_BIN) && \
	LD_LIBRARY_PATH="$(VERDI_HOME)/share/PLI/lib/linux64:$(VERDI_HOME)/share/PLI/IUS/linux64:$(VERDI_HOME)/share/PLI/IUS/linux64/boot:$(LD_LIBRARY_PATH)" \
	xrun $(XRUN_OPTS)