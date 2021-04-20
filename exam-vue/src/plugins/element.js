import Vue from 'vue'
import {
  Button, Container, Main, Row, Footer, Card, FormItem, Form, Input, Message, MessageBox, Aside, MenuItemGroup,
  Menu, MenuItem,Submenu,Col,Header,Tooltip,Breadcrumb,BreadcrumbItem,Dropdown,DropdownItem,DropdownMenu,Notification,
  Tag,TableColumn,Table,Select,Option,Pagination,Dialog,Loading,Upload,Checkbox,CheckboxGroup,DatePicker,Step,Steps,
  RadioGroup,Radio,Alert,InputNumber,Scrollbar
} from 'element-ui'

Vue.prototype.$message = Message
Vue.prototype.$alert = MessageBox.alert
Vue.prototype.$notify = Notification
Vue.prototype.$confirm = MessageBox.confirm
Vue.prototype.$prompt = MessageBox.prompt
Vue.prototype.$Loading = Loading

Vue.use(Button)
Vue.use(Scrollbar)
Vue.use(InputNumber)
Vue.use(Alert)
Vue.use(RadioGroup)
Vue.use(Steps)
Vue.use(Step)
Vue.use(Radio)
Vue.use(DatePicker)
Vue.use(CheckboxGroup)
Vue.use(Checkbox)
Vue.use(Upload)
Vue.use(Dialog)
Vue.use(Loading)
Vue.use(Pagination)
Vue.use(Option)
Vue.use(Select)
Vue.use(Tag)
Vue.use(Table)
Vue.use(TableColumn)
Vue.use(DropdownMenu)
Vue.use(DropdownItem)
Vue.use(Dropdown)
Vue.use(Container)
Vue.use(BreadcrumbItem)
Vue.use(Tooltip)
Vue.use(Main)
Vue.use(Row)
Vue.use(Footer)
Vue.use(Card)
Vue.use(FormItem)
Vue.use(Form)
Vue.use(Input)
Vue.use(Aside)
Vue.use(Menu)
Vue.use(Col)
Vue.use(MenuItem)
Vue.use(MenuItemGroup)
Vue.use(Header)
Vue.use(Submenu)
Vue.use(Breadcrumb)
