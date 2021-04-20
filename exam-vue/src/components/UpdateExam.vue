<template>
  <el-container>
    <el-header height="220">
      <el-steps :active="curStep" simple>
        <el-step title="组卷配置" icon="el-icon-edit"></el-step>
        <el-step title="考试权限" icon="el-icon-lock"></el-step>
        <el-step title="补充配置" icon="el-icon-setting"></el-step>
      </el-steps>

      <el-button style="margin-top: 10px" v-show="curStep !== 1" @click="curStep--">上一步</el-button>

      <el-button style="float:right;margin-top: 10px" v-show="curStep !== 3" type="primary" @click="curStep++">下一步
      </el-button>
      <el-button style="float:right;margin-top: 10px" v-show="curStep === 3" type="primary" @click="updateExam">提交
      </el-button>
    </el-header>

    <el-main>
      <!--设置试题信息-->
      <el-card v-show="curStep === 1">

        <span style="float: right;color: red;font-weight: bold">
          {{ '试卷总分：' + sumTotalScore }}</span>

        <div>
          <el-card>
            <h1>题目列表</h1>
            <el-button type="primary" icon="el-icon-plus" size="small" @click="showAddDialog">添加试题</el-button>

            <el-table v-loading="pageLoading" :data="updateExamQuestion" border style="margin-top: 10px">

              <el-table-column
                type="index"
                label="序号"
                width="50">
              </el-table-column>

              <el-table-column label="题目内容" align="center">
                <template slot-scope="scope">
                  {{ scope.row.questionContent }}
                </template>
              </el-table-column>

              <el-table-column align="center"
                               label="题目类型">
                <template slot-scope="scope">
                  <span v-if="scope.row.questionType === 1">单选题</span>
                  <span v-else-if="scope.row.questionType === 2">多选题</span>
                  <span v-else-if="scope.row.questionType === 3">判断题</span>
                  <span v-else-if="scope.row.questionType === 4">简答题</span>
                </template>
              </el-table-column>

              <el-table-column label="单题分数" align="center">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.score" style="margin-left: 5px"></el-input>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="80" align="center">
                <template slot-scope="scope">
                  <el-button type="danger" icon="el-icon-delete" circle
                             @click="delQuestion(scope.row.questionId)"></el-button>
                </template>
              </el-table-column>
            </el-table>

          </el-card>
        </div>
      </el-card>

      <!--设置考试权限-->
      <el-card v-show="curStep === 2">
        <el-radio-group v-model="examInfo.type" size="medium">
          <el-radio :label="1" border>完全公开</el-radio>
          <el-radio :label="2" border>需要密码</el-radio>
        </el-radio-group>

        <el-alert style="margin-top: 15px"
                  :title="examInfo.type === 1? '开放的，任何人都可以进行考试！' : '半开放的，知道密码的人员才可以考试！'"
                  type="warning">
        </el-alert>

        <el-input style="margin-top: 15px;width: 20%" v-model="examInfo.password" v-show="examInfo.type === 2"
                  type="password" show-password placeholder="输入考试密码"></el-input>
      </el-card>

      <!--设置考试信息-->
      <el-card v-show="curStep === 3">

        <el-form :model="examInfo" :rules="examInfoRules" ref="examInfoForm" label-width="100px">

          <el-form-item label="考试名称" prop="examName">
            <el-input v-model="examInfo.examName"></el-input>
          </el-form-item>

          <el-form-item label="考试描述" prop="examDesc">
            <el-input v-model="examInfo.examDesc"></el-input>
          </el-form-item>

          <el-form-item label="总分数" prop="totalScore">
            <el-input-number :value="sumTotalScore" :disabled="true"></el-input-number>
          </el-form-item>

          <el-form-item label="及格分数" prop="passScore">
            <el-input-number v-model="examInfo.passScore" :min="1"></el-input-number>
          </el-form-item>

          <el-form-item label="考试时长(分钟)" prop="examDuration">
            <el-input-number v-model="examInfo.examDuration" :min="1"></el-input-number>
          </el-form-item>

          <el-form-item label="考试开始时间" prop="startTime">
            <el-date-picker style="margin-left: 5px"
                            v-model="examInfo.startTime"
                            type="date" placeholder="考试开始时间">
            </el-date-picker>
          </el-form-item>

          <el-form-item label="考试结束时间" prop="endTime">
            <el-date-picker style="margin-left: 5px"
                            v-model="examInfo.endTime"
                            type="date" placeholder="考试结束时间">
            </el-date-picker>
          </el-form-item>

        </el-form>

      </el-card>
    </el-main>

    <el-dialog title="添加题目" :visible.sync="showQuestionDialog" width="80%" center>
      <el-row>
        <el-select @change="typeChange" clearable v-model="queryInfo.questionType" placeholder="请选择题目类型">
          <el-option
            v-for="item in questionType"
            :key="item.id"
            :label="item.name"
            :value="item.id">
          </el-option>
        </el-select>

        <el-select @change="bankChange" clearable v-model="queryInfo.questionBank" placeholder="请选择题库"
                   style="margin-left: 5px">
          <el-option
            v-for="item in allBank"
            :key="item.questionBank.bankId"
            :label="item.questionBank.bankName"
            :value="item.questionBank.bankName">
          </el-option>
        </el-select>

        <el-input v-model="queryInfo.questionContent" @blur="getQuestionInfo" placeholder="题目内容"
                  style="margin-left: 5px;width: 220px"
                  prefix-icon="el-icon-search"></el-input>
        <el-button type="primary" @click="addQuToFree" style="float: right">确认{{ selectedTable.length }}项</el-button>
      </el-row>

      <el-table
        ref="questionTable"
        highlight-current-row
        v-loading="loading"
        :border="true"
        :data="questionInfo"
        tooltip-effect="dark"
        style="width: 100%;margin-top: 25px;" @selection-change="handleTableSectionChange">

        <el-table-column align="center"
                         type="selection"
                         width="55">
        </el-table-column>

        <el-table-column align="center"
                         label="题目类型">
          <template slot-scope="scope">
            <span v-if="scope.row.quType === 1">单选题</span>
            <span v-else-if="scope.row.quType === 2">多选题</span>
            <span v-else-if="scope.row.quType === 3">判断题</span>
            <span v-else-if="scope.row.quType === 4">简答题</span>
          </template>
        </el-table-column>

        <el-table-column align="center" label="题目内容">
          <template slot-scope="scope">
            <span class="quContent">{{ scope.row.quContent }}</span>
          </template>
        </el-table-column>

        <el-table-column align="center"
                         label="难度">
          <template slot-scope="scope">
            <span v-if="scope.row.level === 1">简单</span>
            <span v-if="scope.row.level === 2">中等</span>
            <span v-if="scope.row.level === 3">困难</span>
          </template>
        </el-table-column>

        <el-table-column align="center"
                         prop="quBankName"
                         label="所属题库">
        </el-table-column>

        <el-table-column align="center"
                         prop="createPerson"
                         label="创建人">
        </el-table-column>

        <el-table-column align="center"
                         label="创建时间">
          <template slot-scope="scope">
            {{ scope.row.createTime }}
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
    </el-dialog>

  </el-container>
</template>

<script>
  export default {
    name: 'UpdateExam',
    data () {
      return {
        //查询题目的参数
        queryInfo: {
          //题目类型下拉款所选的内容
          'questionType': '',
          'questionBank': '',
          'questionContent': '',
          'pageNo': 1,
          'pageSize': 10
        },
        //题目类型
        questionType: [
          {
            id: 1,
            name: '单选题',
          },
          {
            id: 2,
            name: '多选题',
          },
          {
            id: 3,
            name: '判断题',
          },
          {
            id: 4,
            name: '简答题',
          },
        ],
        //所有题库信息
        allBank: [],
        //当前的步骤
        curStep: 1,
        //考试题目信息
        updateExamQuestion: [],
        //添加题库的对话框
        showQuestionDialog: false,
        //对话框中题目表格的加载
        loading: true,
        //页面中的题目列表表格
        pageLoading: true,
        //所有题目的信息
        questionInfo: [],
        //所有题目的对话框中表格被选中
        selectedTable: [],
        //对话框中题目的总数
        total: 0,
        //当前考试的信息
        examInfo: {},
        //补充的考试信息的表单验证
        examInfoRules: {
          examName: [
            {
              required: true,
              message: '请输入考试名称',
              trigger: 'blur'
            }
          ],
          passScore: [
            {
              required: true,
              message: '请输入通过分数',
              trigger: 'blur'
            }
          ],
          examDuration: [
            {
              required: true,
              message: '请输入考试时长',
              trigger: 'blur'
            }
          ],
        },
      }
    },
    props: ['tagInfo'],
    created () {
      //一创建就改变头部的面包屑
      this.$emit('giveChildChangeBreakInfo', '更新考试', '更新考试')
      this.createTagsInParent()
      this.getExamInfo()
      this.getBankInfo()
    },
    methods: {
      //向父组件中添加头部的tags标签
      createTagsInParent () {
        let flag = false
        this.tagInfo.map(item => {
          //如果tags全部符合
          if (item.name === '更新考试' && item.url === this.$route.path) {
            flag = true
          } else if (item.name === '更新考试' && item.url !== this.$route.path) {
            this.$emit('updateTagInfo', '更新考试', this.$route.path)
            flag = true
          }
        })
        if (!flag) this.$emit('giveChildAddTag', '更新考试', this.$route.path)
      },
      //根据考试的id查询考试的信息和题目的信息
      async getExamInfo () {
        await this.$http.get(this.API.getExamInfoById, { params: this.$route.params }).then((resp) => {
          if (resp.data.code === 200) {
            this.examInfo = resp.data.data
            let scores = resp.data.data.scores.split(',')
            resp.data.data.questionIds.split(',').forEach((item, index) => {
              this.$http.get(this.API.getQuestionById + '/' + item).then((r) => {
                this.updateExamQuestion.push({
                  'questionId': parseInt(item),
                  'questionType': r.data.data.questionType,
                  'questionContent': r.data.data.questionContent,
                  'score': scores[index]
                })
              })
            })
            this.pageLoading = false
          }
        })
      },
      //自由组卷时添加试题
      showAddDialog () {
        this.showQuestionDialog = true
        this.getQuestionInfo()
      },
      //获取所有的题库信息
      getBankInfo () {
        this.$http.get(this.API.getBankHaveQuestionSumByType, {
          params: {
            'pageNo': 1,
            'pageSize': 9999
          }
        }).then((resp) => {
          if (resp.data.code === 200) {
            this.allBank = resp.data.data.bankHaveQuestionSums;
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
      //获取题目信息
      getQuestionInfo () {
        this.$http.get(this.API.getQuestion, { params: this.queryInfo }).then((resp) => {
          if (resp.data.code === 200) {
            this.questionInfo = resp.data.data.questions;
            this.total = resp.data.data.total;
            this.loading = false;
          } else {
            this.$notify({
              title: 'Tips',
              message: '获取题库信息失败',
              type: 'error',
              duration: 2000
            })
          }
        })
      },
      //自由组卷时删除试题
      delQuestion (questionId) {
        this.updateExamQuestion.forEach((item, index) => {
          if (item.questionId === questionId) this.updateExamQuestion.splice(index, 1)
        })
      },
      //题目类型变化
      typeChange (val) {
        this.queryInfo.questionType = val
        this.getQuestionInfo()
      },
      //题库变化
      bankChange (val) {
        this.queryInfo.questionBank = val
        this.getQuestionInfo()
      },
      //自由组卷中选中的题目添加进去
      addQuToFree () {
        this.selectedTable.forEach(item => {
          if (!this.updateExamQuestion.some(i2 => {
            return i2.questionId === item.id
          })) {//不存在有当前题目
            this.updateExamQuestion.push({
              'questionId': item.id,
              'questionContent': item.quContent,
              'questionType': item.quType,
              'score': 1
            })
          }
        })
        this.showQuestionDialog = false
      },
      //处理表格被选中
      handleTableSectionChange (val) {
        this.selectedTable = val
      },
      //分页页面大小改变
      handleSizeChange (val) {
        this.queryInfo.pageSize = val
        this.getQuestionInfo()
      },
      //分页插件的页数
      handleCurrentChange (val) {
        this.queryInfo.pageNo = val
        this.getQuestionInfo()
      },
      //更新考试
      updateExam () {
        this.$refs['examInfoForm'].validate((valid) => {
          if (valid) {
            //构造数据对象(考试信息)
            let exam = this.examInfo
            if (this.updateExamQuestion.length !== 0) {//自由组卷模式
              if (exam.type === 1) {
                exam.password = null
              }
              console.log(exam)
              if (exam.type === 2 && (exam.password === null || exam.password === '')) {
                this.$message.error('当前权限为需要密码,但是密码为空')
                return false
              }
              //题目id数组
              let questionIds = []
              //题目成绩数组
              let scores = []
              this.updateExamQuestion.forEach(item => {
                questionIds.push(item.questionId)
                scores.push(item.score)
              })
              exam.questionIds = questionIds.join(',')
              exam.scores = scores.join(',')
              this.$http.post(this.API.updateExamInfo, exam).then((resp) => {
                if (resp.data.code === 200) {
                  this.$notify({
                    type: 'success',
                    title: 'Tips',
                    message: resp.data.message,
                    duration: 2000
                  })
                  this.$router.push('/examManage')
                }
              })
            } else {
              this.$message.error('请检查考试规则设置是否完整')
            }
          } else {
            this.$message.error('请检查考试规则设置是否完整')
            return false
          }
        })
      }
    },
    computed: {
      //计算总分
      sumTotalScore () {
        let score = 0
        this.updateExamQuestion.forEach(item => {
          score += parseInt(item.score)
        })
        this.examInfo.totalScore = score
        return score
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
</style>
