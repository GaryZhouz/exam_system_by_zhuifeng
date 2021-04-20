<template>
  <el-container>
    <el-header>
      <el-input v-model="queryInfo.bankName" @blur="contentChange" placeholder="题库名称"
                style="width: 220px"
                prefix-icon="el-icon-search"></el-input>
      <br>
      <el-button type="primary" style="margin-top: 10px" icon="el-icon-plus" @click="addTableVisible = true">添加
      </el-button>

    </el-header>

    <el-main style="margin-top: 20px">

      <!--操作的下拉框-->
      <el-select @change="operationChange" clearable v-if="selectedTable.length !== 0" v-model="operation"
                 :placeholder="'已选择' + selectedTable.length + '项'" style="margin-bottom: 25px;">

        <el-option value="delete">
          <span style="float: left">删除</span>
          <span style="float: right; color: #8492a6; font-size: 13px">delete</span>
        </el-option>

      </el-select>

      <el-table
        ref="questionTable"
        highlight-current-row
        v-loading="loading"
        :border="true"
        :data="questionBankInfo"
        tooltip-effect="dark"
        style="width: 100%;" @selection-change="handleTableSectionChange">

        <el-table-column align="center"
                         type="selection"
                         width="55">
        </el-table-column>

        <el-table-column align="center"
                         prop="questionBank.bankName"
                         label="题库名称">
        </el-table-column>

        <el-table-column align="center"
                         prop="singleChoice"
                         label="单选题数量">
        </el-table-column>

        <el-table-column align="center"
                         prop="multipleChoice"
                         label="多选题数量">
        </el-table-column>

        <el-table-column align="center"
                         prop="judge"
                         label="判断题数量">
        </el-table-column>

        <el-table-column align="center"
                         prop="shortAnswer"
                         label="简答题数量">
        </el-table-column>

      </el-table>
      <!--分页-->
      <el-pagination style="margin-top: 25px"
                     @size-change="handleSizeChange"
                     @current-change="handleCurrentChange"
                     :current-page="queryInfo.pageNo"
                     :page-sizes="[10, 20, 30, 50]"
                     :page-size="queryInfo.pageSize"
                     layout="total, sizes, prev, pager, next, jumper"
                     :total="total">
      </el-pagination>

      <!--添加题库信息-->
      <el-dialog title="添加题库" :visible.sync="addTableVisible" width="30%" @close="$refs['addForm'].resetFields()"
                 center>

        <el-form :model="addForm" :rules="addFormRules" ref="addForm">

          <el-form-item label="题库名称" label-width="120px" prop="bankName">
            <el-input v-model="addForm.bankName"></el-input>
          </el-form-item>

        </el-form>

        <div slot="footer" class="dialog-footer">
          <el-button @click="addTableVisible = false">取 消</el-button>
          <el-button type="primary" @click="addQuestionBank">确 定</el-button>
        </div>
      </el-dialog>

    </el-main>
  </el-container>
</template>

<script>
  export default {
    name: 'QuestionBankManage',
    data () {
      return {
        queryInfo: {
          bankName: '',
          pageNo: 1,
          pageSize: 10
        },
        //被选中的表格的信息
        selectedTable: [],
        //所有题库信息
        questionBankInfo: [],
        //当前被选中的操作
        operation: '',
        loading: true,
        //所有的题库条数
        total: 0,
        //添加题库的对话框
        addTableVisible: false,
        //添加题库的表单信息
        addForm: {
          bankName: ''
        },
        //添加表单的数据校验规则
        addFormRules: {
          bankName: [
            {
              required: true,
              message: '请输入题库名称',
              trigger: 'blur'
            },
          ]
        },

      }
    },
    created () {
      this.getBankInfo()
    },
    methods: {
      //获取所有的题库信息
      getBankInfo () {
        this.$http.get(this.API.getBankHaveQuestionSumByType, { params: this.queryInfo }).then((resp) => {
          if (resp.data.code === 200) {
            this.questionBankInfo = resp.data.data.bankHaveQuestionSums
            this.total = resp.data.data.total
            this.loading = false
          } else {
            this.$notify({
              title: 'Tips',
              message: resp.data.message,
              type: 'error',
              duration: 2000
            })
          }
        })
      },
      //查询内容变化
      contentChange () {
        this.getBankInfo()
      },
      //操作选项的被触发
      operationChange (val) {
        if (val === 'delete') {
          this.$confirm('此操作将永久删除该题库, 是否继续?', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            let ids = []
            this.selectedTable.map(item => {
              ids.push(item.questionBank.bankId)
            })
            //发起删除请求
            this.$http.get(this.API.deleteQuestionBank, { params: { 'ids': ids.join(',') } }).then((resp) => {
              if (resp.data.code === 200) {
                this.$notify({
                  title: 'Tips',
                  message: resp.data.message,
                  type: 'success',
                  duration: 2000
                })
                this.getBankInfo()
              } else {
                this.$notify({
                  title: 'Tips',
                  message: resp.data.message,
                  type: 'error',
                  duration: 2000
                })
              }
            })
          }).catch(() => {
            this.$message({
              type: 'info',
              message: '已取消删除'
            })
          })
        }
      },
      //表格部分行被选中
      handleTableSectionChange (row) {
        this.selectedTable = row
      },
      //分页插件的大小改变
      handleSizeChange (val) {
        this.queryInfo.pageSize = val
        this.getBankInfo()
      },
      //分页插件的页数
      handleCurrentChange (val) {
        this.queryInfo.pageNo = val
        this.getBankInfo()
      },
      //添加题库
      addQuestionBank () {
        this.$refs['addForm'].validate((valid) => {
          if (valid) {
            this.$http.post(this.API.addQuestionBank, this.addForm).then((resp) => {
              if (resp.data.code === 200) {
                this.getBankInfo()
                this.$notify({
                  title: 'Tips',
                  message: resp.data.message,
                  type: 'success',
                  duration: 2000
                })
              } else {
                this.$notify({
                  title: 'Tips',
                  message: resp.data.message,
                  type: 'error',
                  duration: 2000
                })
              }
              this.addTableVisible = false
            })
          } else {
            this.$message.error('请检查您所填写的信息是否有误')
            return false
          }
        })
      },
    }
  }
</script>

<style scoped lang="scss">
  .el-container {
    width: 100%;
    height: 100%;
  }

  .el-input {
    width: 200px;
  }

  .el-container {
    animation: leftMoveIn .7s ease-in;
  }

  @keyframes leftMoveIn {
    0% {
      transform: translateX(-100%);
      opacity: 0;
    }
    100% {
      transform: translateX(0%);
      opacity: 1;
    }
  }

  .role {
    color: #606266;
  }

  /deep/ .el-table thead {
    color: rgb(85, 85, 85) !important;
  }

  /*表格的头部样式*/
  /deep/ .has-gutter tr th {
    background: rgb(242, 243, 244);
    color: rgb(85, 85, 85);
    font-weight: bold;
    line-height: 32px;
  }

  .el-table {
    box-shadow: 0 0 1px 1px gainsboro;
  }
</style>
