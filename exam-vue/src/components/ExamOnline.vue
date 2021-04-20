<template>
  <el-container>
    <el-header height="220">
      <el-select @change="typeChange" clearable v-model="queryInfo.examType" placeholder="请选择考试类型">
        <el-option
          v-for="item in examType"
          :key="item.type"
          :label="item.info"
          :value="item.type">
        </el-option>
      </el-select>

      <el-input v-model="queryInfo.examName" placeholder="考试名称" @blur="getExamInfo"
                style="margin-left: 5px;width: 220px"
                prefix-icon="el-icon-search"></el-input>
    </el-header>

    <el-main>

      <el-table
        ref="questionTable"
        highlight-current-row
        v-loading="loading"
        :border="true"
        :data="examInfo"
        tooltip-effect="dark"
        style="width: 100%;">

        <el-table-column align="center" label="考试名称">
          <template slot-scope="scope">
            {{ scope.row.examName }}
          </template>
        </el-table-column>

        <el-table-column align="center" label="考试类型">
          <template slot-scope="scope">
            {{ scope.row.type === 1 ? '完全公开' : '需要密码' }}
          </template>
        </el-table-column>

        <el-table-column align="center" label="考试时间">
          <template slot-scope="scope">
            {{ scope.row.startTime !== 'null' && scope.row.endTime !== 'null' ?
            scope.row.startTime + ' ~' + scope.row.endTime : '不限时'}}
          </template>
        </el-table-column>

        <el-table-column align="center" label="考试时长">
          <template slot-scope="scope">
            {{ scope.row.duration + '分钟' }}
          </template>
        </el-table-column>

        <el-table-column align="center" prop="totalScore" label="考试总分"></el-table-column>

        <el-table-column align="center" prop="passScore" label="及格分数"></el-table-column>

        <el-table-column align="center" label="操作">
          <template slot-scope="scope">
            <el-button size="small" :disabled="!checkExam(scope.row)" @click="toStartExam(scope.row)"
                       :icon="checkExam(scope.row) ? 'el-icon-caret-right' : 'el-icon-close'"
                       :type="checkExam(scope.row) ? 'primary' : 'info'">
              {{ checkExam(scope.row) ? '去考试' : '暂不开放' }}
            </el-button>
          </template>
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
    </el-main>
    <el-dialog
      title="考试提示"
      :visible.sync="startExamDialog" center
      width="50%">

      <el-alert
        title="点击`开始考试`后将自动进入考试，请诚信考试，考试过程中可能会对用户行为、用户视频进行截图采样，请知悉！"
        type="error">
      </el-alert>

      <el-card style="margin-top: 25px">
        <span>考试名称：</span>{{ currentSelectedExam.examName }}
        <br>
        <span>考试描述：</span>{{ currentSelectedExam.examDesc }}
        <br>
        <span>考试时长：</span>{{ currentSelectedExam.duration + '分钟' }}
        <br>
        <span>试卷总分：</span>{{ currentSelectedExam.totalScore }}分
        <br>
        <span>及格分数：</span>{{ currentSelectedExam.passScore }}分
        <br>
        <span>开放类型：</span> {{ currentSelectedExam.type === 2 ? '需要密码' : '完全公开' }}
        <br>
      </el-card>


      <span slot="footer" class="dialog-footer">
    <el-button @click="startExamDialog = false">返 回</el-button>
    <el-button type="primary" @click="$router.push('/exam/'+ currentSelectedExam.examId)">开始考试</el-button>
  </span>
    </el-dialog>
  </el-container>
</template>

<script>
  export default {
    name: 'ExamOnline',
    data () {
      return {
        queryInfo: {
          'examType': null,
          'startTime': null,
          'endTime': null,
          'examName': null,
          'pageNo': 0,
          'pageSize': 10
        },
        //表格是否在加载
        loading: true,
        //考试类型信息
        examType: [
          {
            info: '公开考试',
            type: 1
          },
          {
            info: '需要密码',
            type: 2
          }
        ],
        //考试信息
        examInfo: [],
        //查询到的考试总数
        total: 0,
        //开始考试的提示框
        startExamDialog: false,
        //当前选中的考试的信息
        currentSelectedExam: {}
      }
    },
    created () {
      this.getExamInfo()
    },
    methods: {
      //考试类型搜索
      typeChange (val) {
        this.queryInfo.examType = val
        this.getExamInfo()
      },
      //查询考试信息
      getExamInfo () {
        this.$http.post(this.API.getExamInfo, this.queryInfo).then((resp) => {
          if (resp.data.code === 200) {
            resp.data.data.forEach(item => {
              item.startTime = String(item.startTime).substring(0, 10)
              item.endTime = String(item.endTime).substring(0, 10)
            })
            this.examInfo = resp.data.data
            this.getExamTotal()
            this.loading = false
          }
        })
      },
      //查询考试信息
      getExamTotal () {
        let data = JSON.parse(JSON.stringify(this.queryInfo))
        data.pageNo = 1
        data.pageSize = 9999
        this.$http.post(this.API.getExamInfo, data).then((resp) => {
          if (resp.data.code === 200) {
            this.total = resp.data.data.length
          }
        })
      },
      //分页页面大小改变
      handleSizeChange (val) {
        this.queryInfo.pageSize = val
        this.getExamInfo()
      },
      //分页插件的页数
      handleCurrentChange (val) {
        this.queryInfo.pageNo = val
        this.getExamInfo()
      },
      //去考试准备页面
      toStartExam (row) {
        if (row.type === 2) {
          this.$prompt('请提供考试密码', 'Tips', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
          }).then(({ value }) => {
            if (value === row.password) {
              this.startExamDialog = true
              this.currentSelectedExam = row
            } else {
              this.$message.error('密码错误o(╥﹏╥)o')
            }
          }).catch(() => {
          })
        } else {
          this.startExamDialog = true
          this.currentSelectedExam = row
        }
      }
    },
    computed: {
      //检查考试的合法性
      checkExam (row) {
        return (row) => {
          let date = new Date()
          if (row.status === 2) return false
          if (row.startTime === 'null' && row.endTime === 'null') {
            return true
          } else if (row.startTime === 'null') {
            return date < new Date(row.endTime)
          } else if (row.endTime === 'null') {
            return date > new Date(row.startTime)
          } else if (date > new Date(row.startTime) && date < new Date(row.endTime)) return true
        }
      }
    }
  }
</script>

<style scoped lang="scss">
  .el-container {
    width: 100%;
    height: 100%;
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

  span {
    font-weight: 500;
    display: inline-block;
    font-size: 16px;
    padding: 10px !important;
  }
</style>
