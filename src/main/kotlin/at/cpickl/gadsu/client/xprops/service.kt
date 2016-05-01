package at.cpickl.gadsu.client.xprops

import at.cpickl.gadsu.client.Client
import at.cpickl.gadsu.client.xprops.model.CProp
import at.cpickl.gadsu.client.xprops.model.CPropEnum
import at.cpickl.gadsu.client.xprops.model.CPropTypeCallback
import at.cpickl.gadsu.client.xprops.model.CProps
import at.cpickl.gadsu.client.xprops.model.XProp
import at.cpickl.gadsu.client.xprops.model.XPropEnum
import at.cpickl.gadsu.client.xprops.model.XPropTypeCallback
import at.cpickl.gadsu.client.xprops.model.XPropsRegistry
import at.cpickl.gadsu.client.xprops.persistence.SProp
import at.cpickl.gadsu.client.xprops.persistence.XPropsSqlRepository
import java.util.HashMap
import javax.inject.Inject

interface XPropsService {

    fun read(client: Client): CProps
    fun update(client: Client)

}



class XPropsServiceImpl @Inject constructor(
        private val repo: XPropsSqlRepository
) : XPropsService {

    override fun read(client: Client): CProps {
        val sqlProps = repo.select(client)

        val properties: HashMap<XProp, CProp> = HashMap()
        sqlProps.forEach {
            val cprop = buildCProp(it)
            properties.put(cprop.delegate, cprop)
        }
        return CProps(properties)
    }

    override fun update(client: Client) {
        val sprops = client.cprops.map(::transformCPropToSProp)
        repo.delete(client)
        repo.insert(client, sprops)
    }

    private fun buildCProp(sprop: SProp): CProp {
        val xprop = XPropsRegistry.findByKey(sprop.key)
        return xprop.onType(object: XPropTypeCallback<CProp> {
            override fun onEnum(xprop: XPropEnum): CProp {
                val selectedOpts = sprop.value.split(",").map { XPropsRegistry.findEnumValueByKey(it) }
                return CPropEnum(xprop, selectedOpts)
            }

        })
    }


}

private fun transformCPropToSProp(genericCProp: CProp): SProp {
    return genericCProp.onType(object : CPropTypeCallback<SProp> {
        override fun onEnum(cprop: CPropEnum): SProp {
            return SProp(cprop.key, cprop.clientValue.map { it.key }.joinToString(","))
        }
    })
}