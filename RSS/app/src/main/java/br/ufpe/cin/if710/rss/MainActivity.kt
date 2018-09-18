package br.ufpe.cin.if710.rss

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.util.SortedList
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

open class MainActivity : Activity() {

    private val parser = ParserRSS
    private var conteudoRSS: RecyclerView ?= null
    private var viewAdapter: RssAdapter ?= null
    private var sortedList: SortedList<ItemRSS> ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //iniciando lista
        sortedList = SortedList(ItemRSS::class.java, metodosCallback)
        //iniciando adapter
        viewAdapter = RssAdapter(sortedList)
        //iniciando view
        conteudoRSS = RecyclerView(this).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = viewAdapter
        }
        setContentView(conteudoRSS)
    }

    override fun onStart() {
        super.onStart()
        try {
            //tarefa fora da ui thread para evitar bloqueio
            doAsync {
                val str = getRssFeed(resources.getText(R.string.rssfeed) as String)
                val list = parser.parse(str)
                uiThread {
                    //assim que termina o download a view é populada
                    for (item in list){
                        sortedList!!.add(item)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    //classe padrao do adapter
    private inner class RssAdapter(private val list: SortedList<ItemRSS>?): RecyclerView.Adapter<MyHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
            return MyHolder(layoutInflater.inflate(R.layout.itemlista, parent, false))
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            holder.bind(list?.get(position))
        }

        override fun getItemCount(): Int {
            return list!!.size()
        }
    }
    ///////////////

    //classe padrao do holder
    private inner class MyHolder(item: View) : RecyclerView.ViewHolder(item), View.OnClickListener{

        var title: TextView? = null
        var pubDate: TextView? = null

        init {

            title = item.findViewById(R.id.item_titulo)
            pubDate = item.findViewById(R.id.item_data)

            title!!.setOnClickListener(this)
        }

        fun bind(p: ItemRSS?) {
            title?.text = p?.title
            pubDate?.text = p?.pubDate
        }

        //quando for realizado um clique em algum elemento da lista o link referente ao elemento
        //é iniciado no navegador
        override fun onClick(v: View) {
            val position = this.adapterPosition
            val link = Uri.parse(sortedList?.get(position)?.link)
            val intent = Intent(Intent.ACTION_VIEW, link)
            when {
                intent.resolveActivity(packageManager) != null -> startActivity(intent)
            }
        }
    }

    //classe de download usada como a disponibilizada por padrao, possivel implementacao
    // com uso do downloadmanager será feita
    @Throws(IOException::class)
    private fun getRssFeed(feed: String): String {
        var input: InputStream? = null
        var rssFeed = ""
        try {
            val url = URL(feed)
            val conn = url.openConnection() as HttpURLConnection
            input = conn.inputStream
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var count = 0
            while (count != -1) {
                count = input!!.read(buffer)
                if (count != -1) out.write(buffer, 0, count)
            }
            val response = out.toByteArray()
            rssFeed = String(response, charset("UTF-8"))
        } finally {
            input?.close()
        }
        return rssFeed
    }

    //metodos disponibilizados em sala foram usados por conveniencia
    private var metodosCallback: SortedList.Callback<ItemRSS> = object : SortedList.Callback<ItemRSS>() {
        override fun compare(o1: ItemRSS, o2: ItemRSS): Int {
            return o1.title.compareTo(o2.title)
        }

        override fun onInserted(position: Int, count: Int) {
            viewAdapter?.notifyItemRangeInserted(position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            viewAdapter?.notifyItemRangeRemoved(position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            viewAdapter?.notifyItemMoved(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int) {
            viewAdapter?.notifyItemRangeChanged(position, count)
        }

        override fun areContentsTheSame(oldItem: ItemRSS, newItem: ItemRSS): Boolean {
            return areItemsTheSame(oldItem, newItem)
        }

        override fun areItemsTheSame(item1: ItemRSS, item2: ItemRSS): Boolean {
            return compare(item1, item2) == 0
        }
    }

}
